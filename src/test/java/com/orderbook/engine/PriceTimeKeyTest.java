package com.orderbook.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PriceTimeKeyTest {

    @Test
    void askComparatorDeveOrdenarPorPrecoAscDeTempoAsc() {
        var comparator = PriceTimeKey.askComparator();
        Instant now = Instant.now();

        var cheap = new PriceTimeKey(new BigDecimal("100.00"), now, UUID.randomUUID());
        var expensive = new PriceTimeKey(new BigDecimal("200.00"), now, UUID.randomUUID());

        assertTrue(comparator.compare(cheap, expensive) < 0, "Menor preco deve vir primeiro em asks");
    }

    @Test
    void bidComparatorDeveOrdenarPorPrecoDescDeTempoAsc() {
        var comparator = PriceTimeKey.bidComparator();
        Instant now = Instant.now();

        var cheap = new PriceTimeKey(new BigDecimal("100.00"), now, UUID.randomUUID());
        var expensive = new PriceTimeKey(new BigDecimal("200.00"), now, UUID.randomUUID());

        assertTrue(comparator.compare(expensive, cheap) < 0, "Maior preco deve vir primeiro em bids");
    }

    @Test
    void mesmoPrecoDeveOrdenarPorTempoAsc() {
        var comparator = PriceTimeKey.askComparator();
        Instant earlier = Instant.parse("2026-01-01T00:00:00Z");
        Instant later = Instant.parse("2026-01-01T00:01:00Z");

        var first = new PriceTimeKey(new BigDecimal("150.00"), earlier, UUID.randomUUID());
        var second = new PriceTimeKey(new BigDecimal("150.00"), later, UUID.randomUUID());

        assertTrue(comparator.compare(first, second) < 0, "Ordem mais antiga deve vir primeiro (FIFO)");
    }

    @Test
    void mesmoPrecoETempoDeveDesempatarPorOrderId() {
        var comparator = PriceTimeKey.askComparator();
        Instant now = Instant.now();
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        var key1 = new PriceTimeKey(new BigDecimal("150.00"), now, id1);
        var key2 = new PriceTimeKey(new BigDecimal("150.00"), now, id2);

        assertNotEquals(0, comparator.compare(key1, key2), "Chaves com mesmo preco e tempo devem ser distintas");
    }
}
