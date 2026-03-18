package com.orderbook.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderBookSnapshot(List<Entry> bids, List<Entry> asks) {

    public record Entry(UUID orderId, String userName, BigDecimal price, BigDecimal quantity, String status, Instant createdAt) {}
}
