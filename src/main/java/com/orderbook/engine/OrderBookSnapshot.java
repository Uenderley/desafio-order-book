package com.orderbook.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderBookSnapshot(List<Entry> bids, List<Entry> asks) {

    public record Entry(UUID orderId, BigDecimal price, BigDecimal quantity) {}
}
