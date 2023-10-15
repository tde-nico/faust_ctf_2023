package de.faust.auction;

import de.faust.auction.communication.RPCRemoteReference;
import de.faust.auction.communication.RPCServer;
import de.faust.auction.faults.RPCSemantic;
import de.faust.auction.faults.RPCSemanticType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class RPCRemoteObjectManager {

    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static RPCRemoteObjectManager instance;
    private static final Object lock = new Object();
    private final ConcurrentHashMap<Integer, Object> objectHashMap;
    private final ConcurrentHashMap<Object, Remote> exportedObjects;

    private final ConcurrentHashMap<String, RpcReturnValuePair> clientIDToRpcReturnValuePair;
    private final ConcurrentHashMap<String, Future> deleteClientFutures;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicInteger idCounter;

    private RPCRemoteObjectManager() {
        this.idCounter = new AtomicInteger();
        this.objectHashMap = new ConcurrentHashMap<>();
        this.exportedObjects = new ConcurrentHashMap<>();
        this.clientIDToRpcReturnValuePair = new ConcurrentHashMap<>();
        deleteClientFutures = new ConcurrentHashMap<>();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public static synchronized RPCRemoteObjectManager getInstance() {
        synchronized (lock) {
            if (instance == null) {
                instance = new RPCRemoteObjectManager();
            }
        }
        return instance;
    }

    public synchronized Remote exportObject(Remote object) throws UnknownHostException {
        synchronized (lock) {
            if (instance == null) {
                instance = new RPCRemoteObjectManager();
            }
        }
        int id = idCounter.getAndIncrement();
        ArrayList<Class> intfs = new ArrayList<>();
        Class<?> c = object.getClass();
        do {
            for (Class<?> intf : c.getInterfaces()) {
                // check if interface extends Remote
                if (Remote.class.isAssignableFrom(intf)) {
                    intfs.add(intf);
                }
            }
        } while ((c = c.getSuperclass()) != null);
        Class[] a = new Class[intfs.size()];
        for (int i = 0; i < intfs.size(); i++) {
            a[i] = intfs.get(i);
        }


        String hostaddress = getHostaddress();
        if(hostaddress == null){
            logger.info("no ipv6 found");
            hostaddress = InetAddress.getLocalHost().getHostAddress();
        }
        logger.info("exporting with hostaddress: " + hostaddress);








        InvocationHandler handler = new RPCInvocationHandler(new RPCRemoteReference(hostaddress, id));
        Remote proxy = (Remote) Proxy.newProxyInstance(object.getClass().getClassLoader(), a, handler);
        objectHashMap.put(id, object);
        exportedObjects.put(object, proxy);
        RPCServer server = new RPCServer();
        server.start();
        return proxy;
    }

    private static String getHostaddress() {
        try {
            Enumeration<NetworkInterface> netInts = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netInt : Collections.list(netInts)) {
                if (!netInt.isUp()) {
                    continue;
                }
                for (InetAddress inetAddress : Collections.list(netInt.getInetAddresses())) {
                    if (inetAddress instanceof Inet6Address) {
                        String hostaddress = inetAddress.getHostAddress();
                        if (!hostaddress.startsWith("fd66:666")) {
                            continue;
                        }
                        // remove scope separated with "%"
                        return hostaddress.split("%")[0];
                    }
                }
            }
        }catch(SocketException se){
            logger.info(se.toString());
        }
        return "::1";
    }

    public Remote getStub(Object obj) {
        //logger.info("Asking for proxy to object: " + obj);
        if (obj == null) return null;
        return exportedObjects.get(obj);
    }

    private RPCSemanticType getRPCSemanticType(Method method) {
        RPCSemantic annotation = method.getAnnotation(RPCSemantic.class);
        if(annotation != null){
            RPCSemanticType type = annotation.value();
            //logger.info("rom:" + method.getName() + ": " + type);
            return type;
        }
        return RPCSemanticType.LAST_OF_MANY;
    }

    public Object invokeMethod(int objectID, String genericMethodName, Object[] args, String clientID, String rpcID) throws IllegalAccessException {
        // This method is part of the skeleton. Retrieves the information from the stub about the object on which the method shall be called and returns the result.
        if (instance == null) {
            System.out.println("Initialize RPCRemoteObjectManager first");
            return null;
        }

        Object object = objectHashMap.get(objectID);
        Class<?> c = object.getClass();
        Method m = ReflectionHelper.collectAllRemoteMethodsFromClass(c)
                .filter(method -> method.toGenericString().equals(genericMethodName))
                .findAny()
                .orElse(null);
        if (m == null) return new NoSuchMethodException(genericMethodName);

        RPCSemanticType RPCSemanticType = getRPCSemanticType(m);
        switch (RPCSemanticType) {
            case AT_MOST_ONCE:
                synchronized (this.objectHashMap.get(objectID)) {
                    RpcReturnValuePair pair = this.clientIDToRpcReturnValuePair.get(clientID); // we only save one return value per client ID
                    Object returnValue = null;
                    if (pair == null || !pair.rpcID.equals(rpcID)) { // No return value present
                        try {
                            returnValue = m.invoke(objectHashMap.get(objectID), args);
                        } catch (InvocationTargetException e) {
                            returnValue = e.getCause();
                        }
                        this.clientIDToRpcReturnValuePair.put(clientID, new RpcReturnValuePair(rpcID, returnValue));
                        Future oldFuture = deleteClientFutures.put(clientID, scheduledExecutor.schedule(() -> {
                            clientIDToRpcReturnValuePair.remove(clientID);
                            deleteClientFutures.remove(clientID);
                        }, 30, TimeUnit.SECONDS));
                        if (oldFuture != null)
                            oldFuture.cancel(false);
                    } else{
                        logger.info("Got value from cache.");
                    }
                    // logger.info("Return: " + returnValue);
                    return returnValue;
                }
            case LAST_OF_MANY:
                try {
                    return m.invoke(objectHashMap.get(objectID), args);
                } catch (InvocationTargetException e) {
                    return e.getCause();
                }
            default:
                return new NoSuchMethodException(genericMethodName);
        }
    }

    private record RpcReturnValuePair(String rpcID, Object returnValue) {
    }
}