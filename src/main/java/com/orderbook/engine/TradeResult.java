package com.orderbook.engine;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeResult(
        UUID buyOrderId,
        UUID sellOrderId,
        BigDecimal price,
        BigDecimal quantity
) {}
