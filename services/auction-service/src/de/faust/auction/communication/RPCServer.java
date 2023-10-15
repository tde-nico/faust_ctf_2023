package de.faust.auction.communication;

import de.faust.auction.RPCRemoteObjectManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;
import java.util.logging.Logger;

public class RPCServer {

    public static final int SERVER_PORT = 12346;
    
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) {
        RPCServer server = new RPCServer();
        server.start();
    }

    public RPCServer() {}

    public void start() {
        new AcceptorThread(SERVER_PORT).start();
    }

    private static class AcceptorThread extends Thread{

        private final int port;

        public AcceptorThread(int port){
            this.port = port;
        }
        public void run(){
            ServerSocket listenSocket = null;
            try {
                listenSocket = RMISocketFactory.getSocketFactory().createServerSocket(port);
            } catch (IOException e) {
                System.err.println("new ServerSocket(port): " + e.getMessage());
                System.exit(1);
            }
            //accept incoming connections
            Socket clientSocket = null;
            while (true) {
                try {
                    clientSocket = listenSocket.accept();
                    logger.info("New connection");
                } catch (IOException e) {
                    System.err.println("listenSocket.accept(): " + e.getMessage());
                    continue;
                }
                Thread thread = new WorkerThread(clientSocket);
                thread.start();
            }
        }
    }

    private static class WorkerThread extends Thread {
        private final Socket clientSocket;

        WorkerThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            try {
                RPCObjectConnection objectConnection;
                try {
                    objectConnection = new RPCObjectConnection(this.clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                while (objectConnection.isConnected()) {
                    RPCRequest request;
                    try {
                        request = (RPCRequest) objectConnection.receiveObject();
                    } catch (IOException e) {
                        System.err.println(e);
                        if (!objectConnection.isConnected()) {
                            System.out.println("Client disconnected");
                        }
                        objectConnection.close();
                        return;
                    } catch (ClassNotFoundException e){
                        e.printStackTrace();
                        continue;
                    }
                    RPCRemoteObjectManager remoteManager = RPCRemoteObjectManager.getInstance();

                    Object obj = remoteManager.invokeMethod(request.getObjectID(), request.getMethodName(), request.getArgs(), request.getClientID(), request.getRpcID());
                    RPCResponse response = new RPCResponse(obj, request.getRpcID(), request.getSequenceNumber());
                    objectConnection.sendObject(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


