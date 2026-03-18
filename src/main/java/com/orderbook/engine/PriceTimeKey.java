package com.orderbook.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

public record PriceTimeKey(BigDecimal price, Instant timestamp, UUID orderId) {

    public static Comparator<PriceTimeKey> askComparator() {
        return Comparator
                .comparing(PriceTimeKey::price)
                .thenComparing(PriceTimeKey::timestamp)
                .thenComparing(PriceTimeKey::orderId);
    }

    public static Comparator<PriceTimeKey> bidComparator() {
        return Comparator
                .comparing(PriceTimeKey::price, Comparator.reverseOrder())
                .thenComparing(PriceTimeKey::timestamp)
                .thenComparing(PriceTimeKey::orderId);
    }
}
