package de.faust.auction.model;

import java.io.Serializable;

public interface PaymentMethod extends Serializable {
    public boolean canBuy(int price, String couponCode);
}
