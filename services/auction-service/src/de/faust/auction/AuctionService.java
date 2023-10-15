package de.faust.auction;

import de.faust.auction.faults.RPCSemantic;
import de.faust.auction.faults.RPCSemanticType;
import de.faust.auction.model.AuctionEntry;
import de.faust.auction.model.PaymentMethod;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface AuctionService extends Remote {

	/**
	 * Registers a new auction.
	 *
	 * @param auction The auction to register
	 *                duration  Time in seconds until the auction ends
	 *                handler   The handler to notify when the auction has ended
	 * @return the coupon code in case of an AuctionType.BUY_IT_NOW auction
	 * @throws AuctionException if the duration is negative or an auction
	 *                            with the same name already exists
	 */
	@RPCSemantic(RPCSemanticType.AT_MOST_ONCE)
	public String registerAuction(AuctionEntry auction, int duration, AuctionEventHandler handler) throws AuctionException, RemoteException;

	/**
	 * Retrieves all auctions currently in progress.
	 *
	 * @return The auctions currently in progress.
	 */
	@RPCSemantic(RPCSemanticType.LAST_OF_MANY)
	public AuctionEntry[] getAuctions() throws RemoteException;

	/**
	 * Places a new bid on an AuctionType.NORMAL AuctionEntry.
	 *
	 * @param userName    The name of the bidder
	 * @param auctionName The name of the auction
	 * @param price       The bid
	 * @param handler     The handler to notify if another bid is placed
	 *                    that is higher than this bid
	 * @return <tt>true</tt> if the bid has been placed, otherwise <tt>false</tt>
	 * @throws AuctionException if no auction with the specified name is
	 *                            currently in progress
	 */
	@RPCSemantic(RPCSemanticType.AT_MOST_ONCE)
	public boolean placeBid(String userName, String auctionName, int price, AuctionEventHandler handler) throws AuctionException, RemoteException;

	/**
	 * Buy an AuctionType.BUY_IT_NOW AuctionEntry.
	 *
	 * @param auctionName   The name of the auction
	 * @param paymentMethod The method to pay
	 * @return The contents of the Auction
	 * @throws AuctionException if no auction with the specified name is
	 *                          currently in progress
	 */
	@RPCSemantic(RPCSemanticType.AT_MOST_ONCE)
	public String buy(String auctionName, PaymentMethod paymentMethod) throws AuctionException, RemoteException;
}
