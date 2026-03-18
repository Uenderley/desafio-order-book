package com.orderbook.service;

import com.orderbook.entity.*;
import com.orderbook.exception.InsufficientBalanceException;
import com.orderbook.repository.OrderRepository;
import com.orderbook.repository.UserRepository;
import com.orderbook.repository.WalletRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WalletServiceTest {

    @Inject
    WalletService walletService;

    @Inject
    UserRepository userRepository;

    @Inject
    WalletRepository walletRepository;

    @Inject
    OrderRepository orderRepository;

    // === reserveBalance ===

    @Test
    @Transactional
    void deveReservarSaldoBrlParaOrdemDeCompra() {
        User user = createUserWithWallet("reserve-buy@example.com", "10000.00", "0.00000000");

        walletService.reserveBalance(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        assertEquals(0, new BigDecimal("8500.00").compareTo(wallet.balanceBrl));
        assertEquals(0, new BigDecimal("1500.00").compareTo(wallet.reservedBrl));
    }

    @Test
    @Transactional
    void deveReservarSaldoVibraniumParaOrdemDeVenda() {
        User user = createUserWithWallet("reserve-sell@example.com", "0.00", "50.00000000");

        walletService.reserveBalance(user.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        assertEquals(0, new BigDecimal("40.00000000").compareTo(wallet.balanceVibranium));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(wallet.reservedVibranium));
    }

    @Test
    @Transactional
    void deveLancarExcecaoQuandoSaldoBrlInsuficiente() {
        User user = createUserWithWallet("insuf-brl@example.com", "100.00", "0.00000000");

        assertThrows(InsufficientBalanceException.class, () ->
            walletService.reserveBalance(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"))
        );

        // Saldo nao deve ter sido alterado
        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(wallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.reservedBrl));
    }

    @Test
    @Transactional
    void deveLancarExcecaoQuandoSaldoVibraniumInsuficiente() {
        User user = createUserWithWallet("insuf-vib@example.com", "0.00", "5.00000000");

        assertThrows(InsufficientBalanceException.class, () ->
            walletService.reserveBalance(user.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"))
        );

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        assertEquals(0, new BigDecimal("5.00000000").compareTo(wallet.balanceVibranium));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.reservedVibranium));
    }

    // === releaseBalance ===

    @Test
    @Transactional
    void deveLiberarSaldoBrlAoCancelarOrdemDeCompra() {
        User user = createUserWithWallet("release-buy@example.com", "10000.00", "0.00000000");
        walletService.reserveBalance(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order order = createOrder(user, OrderSide.BUY, "150.00", "10.00000000", "7.00000000");

        walletService.releaseBalance(user.id, order);

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        // reservou 1500, liberou 150*7=1050 → reserved=450, balance=8500+1050=9550
        assertEquals(0, new BigDecimal("9550.00").compareTo(wallet.balanceBrl));
        assertEquals(0, new BigDecimal("450.00").compareTo(wallet.reservedBrl));
    }

    @Test
    @Transactional
    void deveLiberarSaldoVibraniumAoCancelarOrdemDeVenda() {
        User user = createUserWithWallet("release-sell@example.com", "0.00", "50.00000000");
        walletService.reserveBalance(user.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order order = createOrder(user, OrderSide.SELL, "150.00", "10.00000000", "6.00000000");

        walletService.releaseBalance(user.id, order);

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        // reservou 10, liberou 6 (remaining) → reserved=4, balance=40+6=46
        assertEquals(0, new BigDecimal("46.00000000").compareTo(wallet.balanceVibranium));
        assertEquals(0, new BigDecimal("4.00000000").compareTo(wallet.reservedVibranium));
    }

    // === settleTrade ===

    @Test
    @Transactional
    void deveTransferirSaldosNoTrade() {
        User buyer = createUserWithWallet("settle-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("settle-seller@example.com", "0.00", "50.00000000");

        // Reserva saldos (simulando criacao de ordens)
        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("155.00"), new BigDecimal("10.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "155.00", "10.00000000", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000", "10.00000000");

        // Trade executa a R$150 (preco do maker/seller)
        walletService.settleTrade(buyOrder, sellOrder, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Wallet buyerWallet = walletRepository.findByUserId(buyer.id).orElseThrow();
        Wallet sellerWallet = walletRepository.findByUserId(seller.id).orElseThrow();

        // Buyer: reservou 155*10=1550, trade a 150*10=1500, excesso=50
        // balanceBrl: 8450 + 50 (excesso) = 8500
        // reservedBrl: 1550 - 1550 = 0
        // balanceVibranium: 0 + 10 = 10
        assertEquals(0, new BigDecimal("8500.00").compareTo(buyerWallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(buyerWallet.reservedBrl));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(buyerWallet.balanceVibranium));

        // Seller: reservou 10 vibranium
        // reservedVibranium: 10 - 10 = 0
        // balanceBrl: 0 + 150*10 = 1500
        assertEquals(0, new BigDecimal("1500.00").compareTo(sellerWallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(sellerWallet.reservedVibranium));
    }

    @Test
    @Transactional
    void deveTransferirSaldosComMatchParcial() {
        User buyer = createUserWithWallet("partial-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("partial-seller@example.com", "0.00", "50.00000000");

        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("3.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "3.00000000", "3.00000000");

        // Match parcial: apenas 3 unidades
        walletService.settleTrade(buyOrder, sellOrder, new BigDecimal("150.00"), new BigDecimal("3.00000000"));

        Wallet buyerWallet = walletRepository.findByUserId(buyer.id).orElseThrow();
        Wallet sellerWallet = walletRepository.findByUserId(seller.id).orElseThrow();

        // Buyer: reservou 1500, usou 150*3=450, excesso preço=0 (mesmo preco)
        // reservedBrl: 1500 - 450 = 1050
        // balanceBrl: 8500 + 0 = 8500
        // balanceVibranium: 0 + 3 = 3
        assertEquals(0, new BigDecimal("8500.00").compareTo(buyerWallet.balanceBrl));
        assertEquals(0, new BigDecimal("1050.00").compareTo(buyerWallet.reservedBrl));
        assertEquals(0, new BigDecimal("3.00000000").compareTo(buyerWallet.balanceVibranium));

        // Seller: reservou 3, usou 3
        // reservedVibranium: 3 - 3 = 0
        // balanceBrl: 0 + 150*3 = 450
        assertEquals(0, new BigDecimal("450.00").compareTo(sellerWallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(sellerWallet.reservedVibranium));
    }

    @Test
    @Transactional
    void deveDevoverExcessoBrlQuandoPrecoTradeEMenorQuePrecoOrdem() {
        User buyer = createUserWithWallet("excess-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("excess-seller@example.com", "0.00", "50.00000000");

        // Buyer quer comprar a no maximo R$160
        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("160.00"), new BigDecimal("5.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("5.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "160.00", "5.00000000", "5.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "5.00000000", "5.00000000");

        // Trade a R$150 (preco do maker), buyer economiza R$10/unidade
        walletService.settleTrade(buyOrder, sellOrder, new BigDecimal("150.00"), new BigDecimal("5.00000000"));

        Wallet buyerWallet = walletRepository.findByUserId(buyer.id).orElseThrow();

        // Buyer: reservou 160*5=800, trade a 150*5=750
        // excesso: (160-150)*5 = 50
        // balanceBrl: 9200 + 50 = 9250
        // reservedBrl: 800 - 800 = 0
        assertEquals(0, new BigDecimal("9250.00").compareTo(buyerWallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(buyerWallet.reservedBrl));
        assertEquals(0, new BigDecimal("5.00000000").compareTo(buyerWallet.balanceVibranium));
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

    private Order createOrder(User user, OrderSide side, String price, String quantity, String remaining) {
        Order order = new Order();
        order.user = user;
        order.side = side;
        order.price = new BigDecimal(price);
        order.quantity = new BigDecimal(quantity);
        order.remaining = new BigDecimal(remaining);
        order.status = OrderStatus.NEW;
        orderRepository.persist(order);
        return order;
    }
}
