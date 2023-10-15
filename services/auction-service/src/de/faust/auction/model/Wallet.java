package de.faust.auction.model;

import de.faust.auction.Wallets;

public record Wallet(
        String walletId
) implements PaymentMethod {

    @Override
    public boolean canBuy(int price, String couponCode){
        return Wallets.subtractBalance(this, price);
    }
}
