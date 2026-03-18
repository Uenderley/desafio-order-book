package com.orderbook.engine;

import com.orderbook.entity.Order;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeResult(
        UUID buyOrderId,
        UUID sellOrderId,
        Order buyOrder,
        Order sellOrder,
        BigDecimal price,
        BigDecimal quantity
) {}
