package com.orderbook.engine;

import com.orderbook.entity.Order;
import com.orderbook.entity.OrderSide;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class MatchingEngine {

    private final ConcurrentSkipListMap<PriceTimeKey, Order> bids =
            new ConcurrentSkipListMap<>(PriceTimeKey.bidComparator());

    private final ConcurrentSkipListMap<PriceTimeKey, Order> asks =
            new ConcurrentSkipListMap<>(PriceTimeKey.askComparator());

    private final ReentrantLock lock = new ReentrantLock();

    private volatile boolean accepting = true;
    private volatile boolean ready = false;

    public MatchResult submitOrder(Order incoming) {
        if (!accepting) {
            throw new IllegalStateException("Matching engine nao esta aceitando novas ordens");
        }
        lock.lock();
        try {
            List<TradeResult> trades = tryMatch(incoming);

            if (incoming.remaining.compareTo(BigDecimal.ZERO) > 0) {
                insertIntoBook(incoming);
            }

            return new MatchResult(trades);
        } finally {
            lock.unlock();
        }
    }

    public void insertWithoutMatching(Order order) {
        lock.lock();
        try {
            insertIntoBook(order);
        } finally {
            lock.unlock();
        }
    }

    public boolean removeOrder(Order order) {
        lock.lock();
        try {
            PriceTimeKey key = buildKey(order);
            ConcurrentSkipListMap<PriceTimeKey, Order> book =
                    order.side == OrderSide.BUY ? bids : asks;
            return book.remove(key) != null;
        } finally {
            lock.unlock();
        }
    }

    public OrderBookSnapshot getOrderBook() {
        lock.lock();
        try {
            List<OrderBookSnapshot.Entry> bidEntries = bids.values().stream()
                    .map(o -> new OrderBookSnapshot.Entry(o.id, o.user != null ? o.user.name : null, o.price, o.remaining, o.status.name(), o.createdAt))
                    .toList();

            List<OrderBookSnapshot.Entry> askEntries = asks.values().stream()
                    .map(o -> new OrderBookSnapshot.Entry(o.id, o.user != null ? o.user.name : null, o.price, o.remaining, o.status.name(), o.createdAt))
                    .toList();

            return new OrderBookSnapshot(bidEntries, askEntries);
        } finally {
            lock.unlock();
        }
    }

    public int getOrderCount() {
        return bids.size() + asks.size();
    }

    public void clear() {
        lock.lock();
        try {
            bids.clear();
            asks.clear();
            accepting = true;
            ready = false;
        } finally {
            lock.unlock();
        }
    }

    public boolean isAccepting() {
        return accepting;
    }

    public void stopAcceptingOrders() {
        accepting = false;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isResponding() {
        try {
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                lock.unlock();
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public void drainInFlightOperations(Duration timeout) {
        try {
            if (lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<TradeResult> runRecoveryMatching() {
        lock.lock();
        try {
            List<TradeResult> allTrades = new ArrayList<>();

            // Percorre bids do maior preco para o menor
            // Para cada bid, tenta casar com asks
            while (!bids.isEmpty() && !asks.isEmpty()) {
                Map.Entry<PriceTimeKey, Order> bestBid = bids.firstEntry();
                Map.Entry<PriceTimeKey, Order> bestAsk = asks.firstEntry();

                if (!isMatchable(bestBid.getValue(), bestAsk.getValue())) {
                    break;
                }

                // Remove do book e faz matching via submitOrder
                Order bid = bestBid.getValue();
                bids.pollFirstEntry();

                // Reset remaining para resubmeter
                List<TradeResult> trades = tryMatch(bid);
                allTrades.addAll(trades);

                if (bid.remaining.compareTo(BigDecimal.ZERO) > 0) {
                    insertIntoBook(bid);
                }
            }

            return allTrades;
        } finally {
            lock.unlock();
        }
    }

    private List<TradeResult> tryMatch(Order incoming) {
        List<TradeResult> trades = new ArrayList<>();

        ConcurrentSkipListMap<PriceTimeKey, Order> oppositeBook =
                incoming.side == OrderSide.BUY ? asks : bids;

        while (incoming.remaining.compareTo(BigDecimal.ZERO) > 0 && !oppositeBook.isEmpty()) {
            Map.Entry<PriceTimeKey, Order> bestEntry = oppositeBook.firstEntry();
            Order resting = bestEntry.getValue();

            if (!isMatchable(incoming, resting)) {
                break;
            }

            BigDecimal matchQty = incoming.remaining.min(resting.remaining);
            BigDecimal matchPrice = resting.price;

            incoming.remaining = incoming.remaining.subtract(matchQty);
            resting.remaining = resting.remaining.subtract(matchQty);

            Order buyOrder = incoming.side == OrderSide.BUY ? incoming : resting;
            Order sellOrder = incoming.side == OrderSide.SELL ? incoming : resting;

            trades.add(new TradeResult(buyOrder.id, sellOrder.id, buyOrder, sellOrder, matchPrice, matchQty));

            if (resting.remaining.compareTo(BigDecimal.ZERO) == 0) {
                oppositeBook.pollFirstEntry();
            }
        }

        return trades;
    }

    private boolean isMatchable(Order incoming, Order resting) {
        if (incoming.side == OrderSide.BUY) {
            return incoming.price.compareTo(resting.price) >= 0;
        } else {
            return incoming.price.compareTo(resting.price) <= 0;
        }
    }

    private void insertIntoBook(Order order) {
        PriceTimeKey key = buildKey(order);
        if (order.side == OrderSide.BUY) {
            bids.put(key, order);
        } else {
            asks.put(key, order);
        }
    }

    private PriceTimeKey buildKey(Order order) {
        return new PriceTimeKey(order.price, order.createdAt, order.id);
    }
}
