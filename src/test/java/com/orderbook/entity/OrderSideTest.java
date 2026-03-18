package com.orderbook.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderSideTest {

    @Test
    void enumDeveTerDoisValores() {
        assertEquals(2, OrderSide.values().length);
    }

    @Test
    void enumDeveTerBuyESell() {
        assertNotNull(OrderSide.valueOf("BUY"));
        assertNotNull(OrderSide.valueOf("SELL"));
    }
}
