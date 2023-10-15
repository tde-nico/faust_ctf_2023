package de.faust.auction;

import de.faust.auction.communication.RPCConnection;
import de.faust.auction.model.*;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Duration;
import java.time.Instant;

public class AuctionClient extends InteractiveShell implements AuctionEventHandler, Serializable {
    private final String userName;
    protected final String registryHost;
    protected final int registryPort;
    protected AuctionService auctionService;

    protected AuctionEventHandler handler;

    public AuctionClient(String userName, String registryHost, int registryPort) {
        this.userName = userName;
        this.registryHost = registryHost;
        this.registryPort = registryPort;;
    }
    
    // ##################
    // # INITIALIZATION #
    // ##################
    
    public void init() {
        try {
            RPCConnection.enableTimeouts();
            
            RPCRemoteObjectManager.getInstance().exportObject(this);
            // get Auction Service from registry
            Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
            this.auctionService = (AuctionService) registry.lookup("auctionService");
        } catch (RemoteException | NotBoundException | UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // #################
    // # EVENT HANDLER #
    // #################

    @Override
    public void handleEvent(AuctionEventType event, AuctionEntry auction) {
        switch (event) {
            case HIGHER_BID -> System.out.println("Someone has overbid you in the auction " + auction.getName() + ".");
            case AUCTION_WON -> {
                System.out.println("You have won the auction " + auction.getName() + ".");
                System.out.println("Content: " + auction.getContent());
            }
            case AUCTION_END -> System.out.println("Your auction with the name " + auction.getName() + " has ended.");
        }
    }

    // ##################
    // # CLIENT METHODS #
    // ##################

    public void register(String auctionName, String content, int duration, AuctionType type, int startingPrice) throws AuctionException, RemoteException {
        AuctionEntry auction = new AuctionEntry(auctionName, type, 0, null, content, startingPrice);
        String coupon = this.auctionService.registerAuction(auction, duration, this);
        if (coupon != null)
            System.out.println("Coupon " + coupon);
    }

    public void list() throws RemoteException {
        AuctionEntry[] auctions = this.auctionService.getAuctions();
        for (AuctionEntry auction : auctions) {
            var left = Duration.between(Instant.now(), Instant.ofEpochMilli(auction.getEndTimeMillis()));
            System.out.printf("%-15s at %3d € [%-10s] - ends in %s\n", 
                    auction.getName() + ":", auction.getPrice(), auction.getAuctionType(), left);
        }
    }

    public void bid(String auctionName, int price) throws AuctionException, RemoteException {
        boolean success = this.auctionService.placeBid(this.userName, auctionName, price, this);
        if (success) {
            System.out.println("Bid was successful.");
        } else {
            System.out.println("Bid was NOT successful.");
        }
    }
    
    public void buy(String auctionName, PaymentMethod paymentMethod) throws AuctionException, RemoteException {
        String content = this.auctionService.buy(auctionName, paymentMethod);
        if (content == null) {
            System.out.println("Payment NOT successful.");
        } else {
            System.out.println("You bought the following string: " + content);
        }
    }

    // #########
    // # SHELL #
    // #########

    protected boolean processCommand(String[] args) {
        switch (args[0]) {
            case "help", "h" -> System.out.print("""
                    The following commands are available:
                      help
                      register <auction-name> <content> <duration> <N/B> [<starting-price>]
                      list
                      bid   <auction-name> <price>
                      buy/y <auction-name> W/C <wallet-id/coupon>
                      quit
                    """
            );
            case "register", "r" -> {
                if (args.length < 5)
                    throw new IllegalArgumentException("Usage: register <auction-name> <content> <duration> <N/B> [<starting-price>]");
                String name = args[1];
                String content = args[2];
                int duration = Integer.parseInt(args[3]);
                AuctionType type = switch (args[4].charAt(0)) {
                    case 'N', 'n' -> AuctionType.NORMAL;
                    case 'B', 'b' -> AuctionType.BUY_IT_NOW;
                    default -> throw new IllegalArgumentException("Unknown auction type " + args[4]);
                };
                int startingPrice = (args.length > 5) ? Integer.parseInt(args[5]) : 0;
                try {
                    register(name, content, duration, type, startingPrice);
                } catch (AuctionException | RemoteException e) {
                    System.err.println(e.getMessage());
                }
            }
            case "list", "l" -> {
                try {
                    list();
                } catch (RemoteException e) {
                    System.err.println(e.getMessage());
                }
            }
            case "bid", "b" -> {
                if (args.length < 3) throw new IllegalArgumentException("Usage: bid <auction-name> <price>");
                int price = Integer.parseInt(args[2]);
                try {
                    bid(args[1], price);
                } catch (AuctionException | RemoteException e) {
                    System.err.println(e.getMessage());
                }
            }
            case "buy", "y" -> {
                if (args.length < 4) throw new IllegalArgumentException("Usage: buy/y <auction-name> W/C <wallet-id/coupon>");
                PaymentMethod paymentMethod = switch (args[2].charAt(0)) {
                    case 'W', 'w' -> new Wallet(args[3]);
                    case 'C', 'c' -> new Coupon(args[3]);
                    default -> throw new IllegalArgumentException("Unknown payment method " + args[2]);
                };
                try {
                    buy(args[1], paymentMethod);
                } catch (AuctionException | RemoteException e) {
                    System.err.println(e.getMessage());
                }
            }
            case "exit", "quit", "x", "q" -> {
                return false;
            }
            default -> throw new IllegalArgumentException("Unknown command: " + args[0] + "\nUse \"help\" to list available commands");
        }
        return true;
    }


    // ########
    // # MAIN #
    // ########

    public static void main(String[] args) {
        checkArguments(args);
        createAndExecuteClient(args);
    }

    protected static void checkArguments(String[] args) {
        if (args.length < 3) {
            System.err.println("usage: java " + AuctionClient.class.getName() + " <user-name> <registry_host> <registry_port>");
            System.exit(1);
        }
    }

    private static void createAndExecuteClient(String[] args) {
        String userName = args[0];
        String registryHost = args[1];
        int registryPort = Integer.parseInt(args[2]);
        AuctionClient client = new AuctionClient(userName, registryHost, registryPort);
        client.init();
        client.shell();
    }
}
