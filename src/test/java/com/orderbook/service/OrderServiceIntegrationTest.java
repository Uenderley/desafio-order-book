package com.orderbook.service;

import com.orderbook.engine.MatchingEngine;
import com.orderbook.entity.*;
import com.orderbook.repository.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OrderServiceIntegrationTest {

    @Inject
    OrderService orderService;

    @Inject
    UserRepository userRepository;

    @Inject
    WalletRepository walletRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    TradeRepository tradeRepository;

    @Inject
    TransactionHistoryRepository txnRepository;

    @Inject
    MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        matchingEngine.clear();
    }

    @Test
    @Transactional
    void fluxoCompletoMatchExato() {
        User buyer = createUserWithWallet("integ-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("integ-seller@example.com", "0.00", "50.00000000");

        // Seller coloca ordem (fica no book)
        Order sellOrder = orderService.createOrder(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));
        assertEquals(OrderStatus.NEW, sellOrder.status);

        // Buyer coloca ordem (casa com seller)
        Order buyOrder = orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        // Ambas devem estar FILLED
        Order updatedBuy = orderRepository.findById(buyOrder.id);
        Order updatedSell = orderRepository.findById(sellOrder.id);
        assertEquals(OrderStatus.FILLED, updatedBuy.status);
        assertEquals(OrderStatus.FILLED, updatedSell.status);

        // Trade persistido
        List<Trade> trades = tradeRepository.findByOrderId(buyOrder.id);
        assertEquals(1, trades.size());
        assertEquals(0, new BigDecimal("150.00").compareTo(trades.get(0).price));

        // Wallets atualizadas
        Wallet buyerWallet = walletRepository.findByUserId(buyer.id).orElseThrow();
        Wallet sellerWallet = walletRepository.findByUserId(seller.id).orElseThrow();
        assertEquals(0, new BigDecimal("8500.00").compareTo(buyerWallet.balanceBrl));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(buyerWallet.balanceVibranium));
        assertEquals(0, new BigDecimal("1500.00").compareTo(sellerWallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(sellerWallet.reservedVibranium));

        // TransactionHistory para ambos
        assertEquals(1, txnRepository.countByUserId(buyer.id));
        assertEquals(1, txnRepository.countByUserId(seller.id));
    }

    @Test
    @Transactional
    void fluxoCompletoMatchParcial() {
        User buyer = createUserWithWallet("partial-integ-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("partial-integ-seller@example.com", "0.00", "50.00000000");

        // Seller vende 3
        orderService.createOrder(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("3.00000000"));

        // Buyer quer 10 → casa 3, fica 7 no book
        Order buyOrder = orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order updatedBuy = orderRepository.findById(buyOrder.id);
        assertEquals(OrderStatus.PARTIALLY_FILLED, updatedBuy.status);
        assertEquals(0, new BigDecimal("7.00000000").compareTo(updatedBuy.remaining));
    }

    @Test
    @Transactional
    void fluxoCompletoMultiplosMatches() {
        User buyer = createUserWithWallet("multi-buyer@example.com", "10000.00", "0.00000000");
        User seller1 = createUserWithWallet("multi-seller1@example.com", "0.00", "50.00000000");
        User seller2 = createUserWithWallet("multi-seller2@example.com", "0.00", "50.00000000");

        // Dois sellers
        orderService.createOrder(seller1.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("3.00000000"));
        orderService.createOrder(seller2.id, OrderSide.SELL, new BigDecimal("152.00"), new BigDecimal("4.00000000"));

        // Buyer quer 7 a R$155 → casa com ambos
        Order buyOrder = orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("155.00"), new BigDecimal("7.00000000"));

        Order updatedBuy = orderRepository.findById(buyOrder.id);
        assertEquals(OrderStatus.FILLED, updatedBuy.status);

        List<Trade> trades = tradeRepository.findByOrderId(buyOrder.id);
        assertEquals(2, trades.size());

        // Wallet do buyer: reservou 155*7=1085, trades: 150*3 + 152*4 = 450+608=1058, excesso=27
        Wallet buyerWallet = walletRepository.findByUserId(buyer.id).orElseThrow();
        assertEquals(0, new BigDecimal("7.00000000").compareTo(buyerWallet.balanceVibranium));
        // balanceBrl: 10000-1085+27 = 8942
        assertEquals(0, new BigDecimal("8942.00").compareTo(buyerWallet.balanceBrl));
    }

    @Test
    @Transactional
    void fluxoSemMatch() {
        User buyer = createUserWithWallet("nomatch-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("nomatch-seller@example.com", "0.00", "50.00000000");

        // Seller a 155, buyer a 150 → sem match
        orderService.createOrder(seller.id, OrderSide.SELL, new BigDecimal("155.00"), new BigDecimal("10.00000000"));
        Order buyOrder = orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order updatedBuy = orderRepository.findById(buyOrder.id);
        assertEquals(OrderStatus.NEW, updatedBuy.status);
        assertEquals(0, new BigDecimal("10.00000000").compareTo(updatedBuy.remaining));

        List<Trade> trades = tradeRepository.findByOrderId(buyOrder.id);
        assertTrue(trades.isEmpty());
    }

    @Test
    @Transactional
    void cancelamentoAposMatchParcial() {
        User buyer = createUserWithWallet("cancel-partial-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("cancel-partial-seller@example.com", "0.00", "50.00000000");

        // Seller vende 3
        orderService.createOrder(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("3.00000000"));

        // Buyer quer 10, casa 3, remaining=7
        Order buyOrder = orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        // Cancela o remaining
        Order cancelled = orderService.cancelOrder(buyer.id, buyOrder.id);
        assertEquals(OrderStatus.CANCELLED, cancelled.status);

        // Saldo: reservou 1500, usou 450 (3*150), cancela 1050 (7*150)
        Wallet buyerWallet = walletRepository.findByUserId(buyer.id).orElseThrow();
        assertEquals(0, new BigDecimal("9550.00").compareTo(buyerWallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(buyerWallet.reservedBrl));
        assertEquals(0, new BigDecimal("3.00000000").compareTo(buyerWallet.balanceVibranium));
    }

    // === Helpers ===

    private User createUserWithWallet(String email, String brl, String vibranium) {
        User user = new User();
        user.name = "Test User";
        user.email = email;
        userRepository.persist(user);

        Wallet wallet = new Wallet();
        wallet.user = user;
        wallet.balanceBrl = new BigDecimal(brl);
        wallet.balanceVibranium = new BigDecimal(vibranium);
        wallet.reservedBrl = BigDecimal.ZERO;
        wallet.reservedVibranium = BigDecimal.ZERO;
        walletRepository.persist(wallet);

        return user;
    }
}
