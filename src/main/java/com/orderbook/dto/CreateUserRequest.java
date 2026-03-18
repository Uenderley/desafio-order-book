package com.orderbook.dto;

import java.math.BigDecimal;

public class CreateUserRequest {
    public String name;
    public String email;
    public BigDecimal initialBalanceBrl;
    public BigDecimal initialBalanceVibranium;
}
