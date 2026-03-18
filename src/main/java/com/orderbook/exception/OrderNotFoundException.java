package com.orderbook.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Ordem nao encontrada: " + orderId);
    }
}
