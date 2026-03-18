package com.orderbook.dto;

import com.orderbook.entity.OrderSide;

import java.math.BigDecimal;

public class CreateOrderRequest {
    public OrderSide side;
    public BigDecimal price;
    public BigDecimal quantity;
}
