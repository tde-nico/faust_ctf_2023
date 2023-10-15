package de.faust.auction;

import de.faust.auction.communication.*;
import de.faust.auction.faults.RPCSemantic;
import de.faust.auction.faults.RPCSemanticType;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class RPCInvocationHandler implements InvocationHandler, Serializable {
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final ConcurrentHashMap<RPCRemoteReference, RPCObjectConnection> remoteConnections = new ConcurrentHashMap<>();
    private RPCRemoteReference remoteReference;
    private Socket socket;
    private String clientID;
    private static int maxTries = 4;


    public RPCInvocationHandler(RPCRemoteReference remoteReference) {
        this.remoteReference = remoteReference;
        this.socket = null;

    }

    public static void setMaxTries(int maxTries) {
        RPCInvocationHandler.maxTries = maxTries;
    }

    private String generateGuid() {
        return java.util.UUID.randomUUID().toString();
    }

    @Override
    public synchronized Object invoke(Object proxy, Method method, Object[] objects) throws Throwable {
        // save socket to claim same port for the lifetime of this object for identifying the client on server side
        if (this.socket == null) {
            try {
                this.socket = RMISocketFactory.getSocketFactory().createSocket(remoteReference.getHost(), RPCServer.SERVER_PORT);
            } catch (IOException e) {
                throw new RemoteException("connection failed", e);
            }
        }

        //reuse connections
        RPCObjectConnection objectConnection;
        if (clientID == null) {
            clientID = generateGuid();
        }
        objectConnection = remoteConnections.get(remoteReference);
        if (objectConnection == null) {
            objectConnection = new RPCObjectConnection(socket);
            remoteConnections.put(remoteReference, objectConnection);
        }
        //check if the arguments might have already been exported and if so use their stubs
        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                Object obj = RPCRemoteObjectManager.getInstance().getStub(objects[i]);
                if (obj != null) {
                    objects[i] = obj;
                }
            }
        }
        //get rpc semantic for the specified method
        RPCSemantic annotation = method.getAnnotation(RPCSemantic.class);
        RPCSemanticType type;
        if(annotation == null){
            type = RPCSemanticType.LAST_OF_MANY;
        }else{
            type = annotation.value();
        }

        //execute rpc according to the semantic
        String rpcID = generateGuid();
        for (int currSeqNum = 0; currSeqNum < maxTries; currSeqNum++) {
            logger.info("try: " + currSeqNum);
            //System.out.println("try: " + currSeqNum);
            RPCRequest request = new RPCRequest(remoteReference.getObjectID(), method.toGenericString(), objects, clientID, rpcID, currSeqNum);
            RPCResponse recv = null;
            try {
                objectConnection.sendObject(request);
            } catch (SocketException se) {
                logger.info("Reconnecting ..");
                objectConnection.reconnect();
                continue;
            }
            // waiting for the corresponding response until timeout hits
            while (true) {
                try {
                    recv = (RPCResponse) objectConnection.receiveObject();
                } catch (SocketTimeoutException ste) {
                    logger.info("Socket timeout");
                    break;
                } catch (IOException e) {
                    logger.info("Reconnecting .. ");
                    objectConnection.reconnect();
                    break;
                }

                if (!recv.getRpcID().equals(rpcID)) {
                    logger.info("Received response from further call");
                    continue;
                }

                if (type == RPCSemanticType.LAST_OF_MANY && recv.getSequenceNumber() != currSeqNum) {
                    logger.info("response's sequence number is: " + recv.getSequenceNumber() + ", but expected current seq num: " + currSeqNum);
                    continue;
                }
                Object recvObject = recv.getObject();
                if (recvObject instanceof Exception exception) {
                    throw exception;
                }
                return recvObject;
            }
        }
        throw new RemoteException("Server does not respond. Try again later.");
    }
}
