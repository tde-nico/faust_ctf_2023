package de.faust.auction;

import de.faust.auction.model.Wallet;

import java.util.concurrent.*;

public final class Wallets {
    private Wallets() {}
    
    public static final int DEFAULT_BALANCE = 10;
    
    private static final ConcurrentHashMap<String, Integer> balances = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Future> deleteFutures = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    
    public static boolean subtractBalance(Wallet wallet, int amount) {
        String walletId = wallet.walletId();
        if (walletId == null || walletId.isEmpty() || walletId.isBlank()) {
            throw new IllegalArgumentException("Illegal wallet id");
        }

        boolean[] result = new boolean[1];
        balances.compute(walletId, (id, balance) -> {
           if (balance == null) {
               balance = DEFAULT_BALANCE;
           }
            if (balance > 0 && amount > 0 && balance >= amount) {
                result[0] = true;
                balance -= amount;        
            } else {
                result[0] = false;
            }
            return balance;
        });
        Future oldFuture = deleteFutures.put(walletId, scheduledExecutor.schedule(() -> {
            balances.remove(walletId);
            deleteFutures.remove(walletId);
        }, AuctionServiceImpl.MAX_DURATION, TimeUnit.SECONDS));
        if (oldFuture != null)
            oldFuture.cancel(false);
        return result[0];
    }
}
