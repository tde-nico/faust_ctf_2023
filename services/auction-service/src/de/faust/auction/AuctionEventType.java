package de.faust.auction;


public enum AuctionEventType {

	/* Used by the service to notify a client that the client's bid
	 * is no longer the highest bid for a particular auction. */
	HIGHER_BID,

	/* Used by the service to notify the creator of an auction
	 * that the auction has ended. */
	AUCTION_END,

	/* Used by the service to notify a client that the client has
	 * won a particular auction, that is, that the auction has
	 * ended and that the client's bid was the highest. */
	AUCTION_WON

}
