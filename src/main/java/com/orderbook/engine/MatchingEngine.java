package com.orderbook.engine;

import com.orderbook.entity.Order;
import com.orderbook.entity.OrderSide;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class MatchingEngine {

    private final ConcurrentSkipListMap<PriceTimeKey, Order> bids =
            new ConcurrentSkipListMap<>(PriceTimeKey.bidComparator());

    private final ConcurrentSkipListMap<PriceTimeKey, Order> asks =
            new ConcurrentSkipListMap<>(PriceTimeKey.askComparator());

    private final ReentrantLock lock = new ReentrantLock();

    public MatchResult submitOrder(Order incoming) {
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
                    .map(o -> new OrderBookSnapshot.Entry(o.id, o.price, o.remaining))
                    .toList();

            List<OrderBookSnapshot.Entry> askEntries = asks.values().stream()
                    .map(o -> new OrderBookSnapshot.Entry(o.id, o.price, o.remaining))
                    .toList();

            return new OrderBookSnapshot(bidEntries, askEntries);
        } finally {
            lock.unlock();
        }
    }

    public int getOrderCount() {
        return bids.size() + asks.size();
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

            UUID buyOrderId = incoming.side == OrderSide.BUY ? incoming.id : resting.id;
            UUID sellOrderId = incoming.side == OrderSide.SELL ? incoming.id : resting.id;

            trades.add(new TradeResult(buyOrderId, sellOrderId, matchPrice, matchQty));

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
