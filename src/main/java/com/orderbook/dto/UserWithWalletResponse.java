package com.orderbook.dto;

import com.orderbook.entity.User;
import com.orderbook.entity.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserWithWalletResponse(
        UUID id,
        String name,
        String email,
        Instant createdAt,
        WalletInfo wallet
) {
    public record WalletInfo(
            BigDecimal balanceBrl,
            BigDecimal balanceVibranium,
            BigDecimal reservedBrl,
            BigDecimal reservedVibranium
    ) {}

    public static UserWithWalletResponse from(User user, Wallet wallet) {
        return new UserWithWalletResponse(
                user.id,
                user.name,
                user.email,
                user.createdAt,
                new WalletInfo(
                        wallet.balanceBrl,
                        wallet.balanceVibranium,
                        wallet.reservedBrl,
                        wallet.reservedVibranium
                )
        );
    }
}
