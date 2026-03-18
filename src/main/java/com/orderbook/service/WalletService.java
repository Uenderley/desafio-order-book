package com.orderbook.service;

import com.orderbook.entity.*;
import com.orderbook.exception.InsufficientBalanceException;
import com.orderbook.repository.WalletRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class WalletService {

    @Inject
    WalletRepository walletRepository;

    public void reserveBalance(UUID userId, OrderSide side, BigDecimal price, BigDecimal quantity) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet nao encontrada para userId: " + userId));

        if (side == OrderSide.BUY) {
            BigDecimal amount = price.multiply(quantity);
            if (wallet.balanceBrl.compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        "Saldo BRL insuficiente. Disponivel: " + wallet.balanceBrl + ", necessario: " + amount);
            }
            wallet.balanceBrl = wallet.balanceBrl.subtract(amount);
            wallet.reservedBrl = wallet.reservedBrl.add(amount);
        } else {
            if (wallet.balanceVibranium.compareTo(quantity) < 0) {
                throw new InsufficientBalanceException(
                        "Saldo Vibranium insuficiente. Disponivel: " + wallet.balanceVibranium + ", necessario: " + quantity);
            }
            wallet.balanceVibranium = wallet.balanceVibranium.subtract(quantity);
            wallet.reservedVibranium = wallet.reservedVibranium.add(quantity);
        }
    }

    public void releaseBalance(UUID userId, Order order) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet nao encontrada para userId: " + userId));

        if (order.side == OrderSide.BUY) {
            BigDecimal amount = order.price.multiply(order.remaining);
            wallet.reservedBrl = wallet.reservedBrl.subtract(amount);
            wallet.balanceBrl = wallet.balanceBrl.add(amount);
        } else {
            wallet.reservedVibranium = wallet.reservedVibranium.subtract(order.remaining);
            wallet.balanceVibranium = wallet.balanceVibranium.add(order.remaining);
        }
    }

    public void settleTrade(Order buyOrder, Order sellOrder, BigDecimal tradePrice, BigDecimal tradeQuantity) {
        // Buyer
        Wallet buyerWallet = walletRepository.findByUserId(buyOrder.user.id)
                .orElseThrow();

        BigDecimal reservedAmount = buyOrder.price.multiply(tradeQuantity);
        BigDecimal excessBrl = buyOrder.price.subtract(tradePrice).multiply(tradeQuantity);

        buyerWallet.reservedBrl = buyerWallet.reservedBrl.subtract(reservedAmount);
        buyerWallet.balanceBrl = buyerWallet.balanceBrl.add(excessBrl);
        buyerWallet.balanceVibranium = buyerWallet.balanceVibranium.add(tradeQuantity);

        // Seller
        Wallet sellerWallet = walletRepository.findByUserId(sellOrder.user.id)
                .orElseThrow();

        sellerWallet.reservedVibranium = sellerWallet.reservedVibranium.subtract(tradeQuantity);
        sellerWallet.balanceBrl = sellerWallet.balanceBrl.add(tradePrice.multiply(tradeQuantity));
    }
}
