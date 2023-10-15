package de.faust.auction;

import de.faust.auction.communication.RPCConnection;

import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AuctionServer {
    public static void main(String[] args) throws AlreadyBoundException, RemoteException, UnknownHostException {
        // check cmd line arguments
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        if (args.length != 1) {
            System.err.println("usage: java " + AuctionServer.class.getName() + " <registry_port>");
            System.exit(1);
        }
        int registryPort = Integer.parseInt(args[0]);
        String serviceName = "auctionService";

        RPCConnection.enableTimeouts();

        Registry registry = LocateRegistry.createRegistry(registryPort);
        // export remote object
        AuctionService auctionServiceImpl = new AuctionServiceImpl(registry);
        AuctionService auctionService = (AuctionService) RPCRemoteObjectManager.getInstance().exportObject(auctionServiceImpl);

        // register remote object in registry
        registry.bind(serviceName, auctionService);
        // keep service running
        System.out.println("Auction Server running.");
    }
}
