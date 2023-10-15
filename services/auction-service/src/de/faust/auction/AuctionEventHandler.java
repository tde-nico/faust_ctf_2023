package de.faust.auction;

import de.faust.auction.faults.RPCSemantic;
import de.faust.auction.faults.RPCSemanticType;
import de.faust.auction.model.AuctionEntry;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface AuctionEventHandler extends Remote {

	/**
	 * Notifies the event handler about an auction event.
	 **
	 * @param event   The type of the event
	 * @param auction The auction
	 */
	@RPCSemantic(RPCSemanticType.AT_MOST_ONCE)
	public void handleEvent(AuctionEventType event, AuctionEntry auction) throws RemoteException;

}
