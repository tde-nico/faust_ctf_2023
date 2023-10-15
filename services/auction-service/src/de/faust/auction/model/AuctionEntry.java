package de.faust.auction.model;

import de.faust.auction.AuctionEventHandler;

import java.io.*;

public class AuctionEntry implements Serializable, Comparable<AuctionEntry> {

    /* The auction name. */
    private final String name;

    private final AuctionType auctionType;

    private final long endTimeMillis;
    
    private final String couponCode; 
    private final String content;
    
    /* The currently highest bid for this auction. */
    private int price;

    /* Handler to notify when the auction has ended. */
    private transient AuctionEventHandler ownerHandler; // transient => handler will not be Serialized and sent to potential malignant client
    private transient String highestBidderName;
    private transient AuctionEventHandler highestBidderHandler; // Handler to notify if another bid is placed that is higher than this bid or when the auction was won.

    public AuctionEntry(String name, AuctionType auctionType, long endTimeMillis, String couponCode, String content, int startingPrice) {
        this.name = name;
        this.auctionType = auctionType;
        this.endTimeMillis = endTimeMillis;
        this.couponCode = couponCode;
        this.content = content;
        this.price = startingPrice;
    }
    
    public AuctionEntry censored() {
        return new AuctionEntry(name, auctionType, endTimeMillis, null, null, price);
    }

    public String getName() {
        return name;
    }

    public AuctionType getAuctionType() {
        return auctionType;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getContent() {
        return content;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int newPrice) {
        this.price = newPrice;
    }

    public AuctionEventHandler getOwnerHandler() {
        return ownerHandler;
    }

    public void setOwnerHandler(AuctionEventHandler ownerHandler) {
        this.ownerHandler = ownerHandler;
    }

    public String getHighestBidderName() {
        return highestBidderName;
    }

    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }

    public AuctionEventHandler getHighestBidderHandler() {
        return highestBidderHandler;
    }

    public void setHighestBidderHandler(AuctionEventHandler highestBidderHandler) {
        this.highestBidderHandler = highestBidderHandler;
    }

    public long getEndTimeMillis() {
        return this.endTimeMillis;
    }

    public boolean hasEnded() {
        return System.currentTimeMillis() >= this.getEndTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AuctionEntry)) {
            return false;
        }
        return ((AuctionEntry) o).getName().equals(name);
    }

    @Override
    public int compareTo(AuctionEntry o) {
        if (this.getEndTimeMillis() < o.getEndTimeMillis()) return -1;
        else if (this.getEndTimeMillis() > o.getEndTimeMillis()) return 1;
        return 0;
    }
}
