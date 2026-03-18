package com.orderbook.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void enumDeveTerQuatroValores() {
        assertEquals(4, OrderStatus.values().length);
    }

    @Test
    void enumDeveTerTodosOsStatus() {
        assertNotNull(OrderStatus.valueOf("NEW"));
        assertNotNull(OrderStatus.valueOf("PARTIALLY_FILLED"));
        assertNotNull(OrderStatus.valueOf("FILLED"));
        assertNotNull(OrderStatus.valueOf("CANCELLED"));
    }

    @Test
    void statusAbertosDevemSerNewEPartiallyFilled() {
        assertTrue(OrderStatus.NEW.isOpen());
        assertTrue(OrderStatus.PARTIALLY_FILLED.isOpen());
        assertFalse(OrderStatus.FILLED.isOpen());
        assertFalse(OrderStatus.CANCELLED.isOpen());
    }
}
