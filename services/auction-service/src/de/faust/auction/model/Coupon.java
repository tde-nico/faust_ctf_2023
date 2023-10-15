package de.faust.auction.model;

public record Coupon (
        String couponCode
) implements PaymentMethod {
    @Override
    public boolean canBuy(int price, String couponCode) {
        return couponCode.equals(this.couponCode);
    }
}
