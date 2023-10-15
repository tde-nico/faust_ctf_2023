package de.faust.auction;

import de.faust.auction.model.AuctionEntry;
import de.faust.auction.model.AuctionType;
import de.faust.auction.model.PaymentMethod;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionServiceImpl implements AuctionService {
    public static final String auctionsDBPath = "data/auctions.db";

    public static final int MAX_DURATION = (int) TimeUnit.MINUTES.toSeconds(7 * 3); // 7 ticks
    public static final int SAVE_INTERVAL = 6;

    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final ScheduledExecutorService scheduledExecutor;
    private final ConcurrentHashMap<String, AuctionEntry> auctions; //Dictionary that maps auction names to AuctionEntry objects

    public AuctionServiceImpl(Registry registry) {
        super();
        if (registry == null) {
            throw new NullPointerException("Registry is null");
        }
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        auctions = load();
        scheduledExecutor.scheduleWithFixedDelay(this::save, SAVE_INTERVAL, SAVE_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Registers a new auction.
     *
     * @param auction The auction to register
     *                duration  Time in seconds until the auction ends
     *                handler   The handler to notify when the auction has ended
     * @return the coupon code in case of an AuctionType.BUY_IT_NOW auction
     * @throws AuctionException if the duration is negative or an auction
     *                          with the same name already exists
     */
    @Override
    public String registerAuction(AuctionEntry auction, int duration, AuctionEventHandler handler) throws AuctionException, RemoteException {
        if (duration <= 0) {
            throw new AuctionException("Duration must be greater than 0.");
        }
        if (duration > MAX_DURATION) {
            throw new AuctionException("Duration must not be greater than " + MAX_DURATION);
        }
        if (auction.getName() == null || auction.getName().isBlank() || auction.getName().isEmpty()) {
            throw new AuctionException("Auction's name must not be null, empty or blank.");
        }
        if (auctions.containsKey(auction.getName())) {
            throw new AuctionException("Auction with same name already exists");
        }
        if (auction.getContent() == null || auction.getContent().isBlank() || auction.getContent().isEmpty()) {
            throw new AuctionException("Auction's content must not be null, empty or blank.");
        }
        String couponCode = null;
        switch (auction.getAuctionType()) {
            case NORMAL -> {
                if (auction.getPrice() < 0) {
                    throw new AuctionException("Price must be at least 0.");
                }
            }
            case BUY_IT_NOW -> {
                couponCode = generateRandomString(128);
                if (auction.getPrice() <= 0) {
                    throw new AuctionException("Price must be greater than 0.");
                }
            }
        }
        AuctionEntry newAuction = new AuctionEntry(auction.getName(),
                auction.getAuctionType(),
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(duration),
                couponCode, auction.getContent(), auction.getPrice());
        newAuction.setOwnerHandler(handler);
        if (this.auctions.putIfAbsent(newAuction.getName(), newAuction) != null) {
            throw new AuctionException("Auction with same name already exists");
        }
        scheduledExecutor.schedule(() -> onAuctionEnd(auction.getName()), duration, TimeUnit.SECONDS);
        return couponCode;
    }

    /**
     * Retrieves all auctions currently in progress.
     *
     * @return The auctions currently in progress.
     */
    @Override
    public AuctionEntry[] getAuctions() throws RemoteException {
        return auctions.values()
                .stream()
                .map(AuctionEntry::censored)
                .toArray(AuctionEntry[]::new);
    }

    /**
     * Places a new bid.
     *
     * @param userName    The name of the bidder
     * @param auctionName The name of the auction
     * @param price       The bid
     * @param handler     The handler to notify if another bid is placed that is higher than this bid
     * @return <tt>true</tt> if the bid has been placed, otherwise <tt>false</tt>
     * @throws AuctionException if no auction with the specified name is
     *                          currently in progress
     */
    @Override
    public synchronized boolean placeBid(String userName, String auctionName, int price, AuctionEventHandler handler) throws AuctionException, RemoteException {
        AuctionEntry auction = auctions.get(auctionName);
        if (auction == null || auction.getAuctionType() != AuctionType.NORMAL) {
            throw new AuctionException("Auction not found");
        } 
        if (auction.hasEnded() || auction.getPrice() >= price) {
            return false;
        }
        synchronized (auction) {
            if (auction.hasEnded() || auction.getPrice() >= price) {
                return false;
            }
            auction.setPrice(price);
            AuctionEventHandler oldHighestBidderHandler = auction.getHighestBidderHandler();
            if (oldHighestBidderHandler != null && !auction.getHighestBidderName().equals(userName)) {
                AuctionEntry censored = auction.censored();
                scheduledExecutor.submit(() -> {
                    try {
                        oldHighestBidderHandler.handleEvent(AuctionEventType.HIGHER_BID, censored);
                    } catch (Exception e) {
                        logger.info(e.toString());
                        logger.info("Could not notify old highest bidder of auction " + censored.getName() + ".");
                    }
                });
            }
            auction.setHighestBidderName(userName);
            auction.setHighestBidderHandler(handler); //set new highest bidder
            return true;
        }
    }

    /**
     * Buy an AuctionType.BUY_IT_NOW AuctionEntry.
     *
     * @param auctionName   The name of the auction
     * @param paymentMethod The method to pay
     * @return The contents of the Auction
     * @throws AuctionException if no auction with the specified name is
     *                          currently in progress
     */
    @Override
    public String buy(String auctionName, PaymentMethod paymentMethod) throws AuctionException, RemoteException {
        AuctionEntry auction = auctions.get(auctionName);
        if (auction == null || auction.getAuctionType() != AuctionType.BUY_IT_NOW) {
            throw new AuctionException("Auction not found");
        } 
        if (auction.hasEnded() || !paymentMethod.canBuy(auction.getPrice(), auction.getCouponCode())) {
            return null;
        }
        return auction.getContent();
    }

    private String generateRandomString(int length) {
        String uuidString = UUID.randomUUID().toString().replace("-", "");
        return uuidString.substring(0, Math.min(length, uuidString.length()));
    }

    protected void onAuctionEnd(String auctionName) {
        AuctionEntry auction = auctions.get(auctionName);
        if (auction == null) {
            return; // already deleted
        }
        if (!auction.hasEnded()) {
            long millisLeft = auction.getEndTimeMillis() - System.currentTimeMillis();
            scheduledExecutor.schedule(() -> onAuctionEnd(auctionName), millisLeft, TimeUnit.MILLISECONDS);
            return;
        }
        auctions.remove(auctionName);
        if (auction.getOwnerHandler() != null) {
            try {
                auction.getOwnerHandler().handleEvent(AuctionEventType.AUCTION_END, auction);
            } catch (Exception e) {
                logger.info(e.toString());
                logger.info("Could not notify owner of auction " + auction.getName() + ".");
            }
        }
        synchronized (auction) {
            if (auction.getHighestBidderHandler() != null) {
                try {
                    auction.getHighestBidderHandler().handleEvent(AuctionEventType.AUCTION_WON, auction);
                } catch (Exception e) {
                    logger.info(e.toString());
                    logger.info("Could not notify winner of auction " + auction.getName() + ".");
                }
            }
        }
    }

    protected synchronized void save() {
        try {
            File file = new File(auctionsDBPath + ".new");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(auctions.values().toArray(AuctionEntry[]::new));
            }
            file.renameTo(new File(auctionsDBPath));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save " + e);
        }
    }

    protected synchronized ConcurrentHashMap<String, AuctionEntry> load() {
        ConcurrentHashMap<String, AuctionEntry> result = new ConcurrentHashMap<>();
        try {
            File file = new File(auctionsDBPath);
            if (!file.isFile() || file.length() == 0) {
                return result;
            }
            AuctionEntry[] arr;
            try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
                arr = (AuctionEntry[]) input.readObject();
            }
            Arrays.stream(arr).forEach(auction -> {
                if (!auction.hasEnded()) {
                     scheduledExecutor.schedule(() -> onAuctionEnd(auction.getName()),
                             auction.getEndTimeMillis() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                     result.put(auction.getName(), auction);
                }
            });
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot load " + auctionsDBPath + " " + e);
        }
        return result;
    }
}
