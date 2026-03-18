package com.orderbook.entity;

public enum OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED;

    public boolean isOpen() {
        return this == NEW || this == PARTIALLY_FILLED;
    }
}
