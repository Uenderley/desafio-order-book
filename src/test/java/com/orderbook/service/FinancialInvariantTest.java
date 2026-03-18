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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class FinancialInvariantTest {

    @Inject
    OrderService orderService;

    @Inject
    UserRepository userRepository;

    @Inject
    WalletRepository walletRepository;

    @Inject
    MatchingEngine matchingEngine;

    private final List<UUID> testUserIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        matchingEngine.clear();
        testUserIds.clear();
    }

    @Test
    @Transactional
    void invariantesAposMatchExato() {
        BigDecimal totalBrl = new BigDecimal("10000.00");
        BigDecimal totalVibranium = new BigDecimal("50.00000000");

        User buyer = createUserWithWallet("inv-exact-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("inv-exact-seller@example.com", "0.00", "50.00000000");

        orderService.createOrder(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));
        orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        assertFinancialInvariants(totalBrl, totalVibranium);
    }

    @Test
    @Transactional
    void invariantesAposMultiplosMatches() {
        BigDecimal totalBrl = new BigDecimal("20000.00");
        BigDecimal totalVibranium = new BigDecimal("100.00000000");

        User buyer = createUserWithWallet("inv-multi-buyer@example.com", "20000.00", "0.00000000");
        User seller1 = createUserWithWallet("inv-multi-seller1@example.com", "0.00", "50.00000000");
        User seller2 = createUserWithWallet("inv-multi-seller2@example.com", "0.00", "50.00000000");

        orderService.createOrder(seller1.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("3.00000000"));
        orderService.createOrder(seller2.id, OrderSide.SELL, new BigDecimal("152.00"), new BigDecimal("4.00000000"));

        // Buyer compra 7 a R$155 — casa com ambos. Excesso BRL devolvido.
        orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("155.00"), new BigDecimal("7.00000000"));

        assertFinancialInvariants(totalBrl, totalVibranium);
    }

    @Test
    @Transactional
    void invariantesAposCancelamentoParcial() {
        BigDecimal totalBrl = new BigDecimal("10000.00");
        BigDecimal totalVibranium = new BigDecimal("50.00000000");

        User buyer = createUserWithWallet("inv-cancel-partial-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("inv-cancel-partial-seller@example.com", "0.00", "50.00000000");

        orderService.createOrder(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("3.00000000"));
        Order buyOrder = orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        // Cancela remaining do buyer (7 unidades)
        orderService.cancelOrder(buyer.id, buyOrder.id);

        assertFinancialInvariants(totalBrl, totalVibranium);
    }

    @Test
    @Transactional
    void invariantesAposCancelamentoTotal() {
        BigDecimal totalBrl = new BigDecimal("10000.00");
        BigDecimal totalVibranium = new BigDecimal("50.00000000");

        User buyer = createUserWithWallet("inv-cancel-total-buyer@example.com", "10000.00", "0.00000000");
        createUserWithWallet("inv-cancel-total-seller@example.com", "0.00", "50.00000000");

        // Buyer cria e cancela sem match
        Order buyOrder = orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("100.00"), new BigDecimal("5.00000000"));
        orderService.cancelOrder(buyer.id, buyOrder.id);

        // Saldos devem voltar ao original
        assertFinancialInvariants(totalBrl, totalVibranium);
    }

    @Test
    @Transactional
    void invariantesComMultiplosUsuariosETrades() {
        BigDecimal totalBrl = BigDecimal.ZERO;
        BigDecimal totalVibranium = BigDecimal.ZERO;

        List<User> buyers = new ArrayList<>();
        List<User> sellers = new ArrayList<>();

        // 5 buyers com 10000 BRL cada
        for (int i = 0; i < 5; i++) {
            User buyer = createUserWithWallet("inv-complex-buyer" + i + "@example.com", "10000.00", "0.00000000");
            buyers.add(buyer);
            totalBrl = totalBrl.add(new BigDecimal("10000.00"));
        }

        // 5 sellers com 100 Vibranium cada
        for (int i = 0; i < 5; i++) {
            User seller = createUserWithWallet("inv-complex-seller" + i + "@example.com", "0.00", "100.00000000");
            sellers.add(seller);
            totalVibranium = totalVibranium.add(new BigDecimal("100.00000000"));
        }

        // Cada seller coloca 10 Vibranium a precos variados
        for (int i = 0; i < 5; i++) {
            BigDecimal price = new BigDecimal(150 + i * 2);
            orderService.createOrder(sellers.get(i).id, OrderSide.SELL, price, new BigDecimal("10.00000000"));
        }

        // Cada buyer compra 10 Vibranium a R$160 (casa com todos os sellers)
        for (int i = 0; i < 5; i++) {
            orderService.createOrder(buyers.get(i).id, OrderSide.BUY, new BigDecimal("160.00"), new BigDecimal("10.00000000"));
        }

        assertFinancialInvariants(totalBrl, totalVibranium);
    }

    @Test
    @Transactional
    void invariantesComPrecoExecucaoMenorQuePrecoOrdem() {
        BigDecimal totalBrl = new BigDecimal("10000.00");
        BigDecimal totalVibranium = new BigDecimal("50.00000000");

        User buyer = createUserWithWallet("inv-price-diff-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("inv-price-diff-seller@example.com", "0.00", "50.00000000");

        // Seller a 140, buyer a 160 → executa a 140, excesso devolvido
        orderService.createOrder(seller.id, OrderSide.SELL, new BigDecimal("140.00"), new BigDecimal("10.00000000"));
        orderService.createOrder(buyer.id, OrderSide.BUY, new BigDecimal("160.00"), new BigDecimal("10.00000000"));

        assertFinancialInvariants(totalBrl, totalVibranium);

        // Verifica que buyer recebeu excesso
        Wallet buyerWallet = walletRepository.findByUserId(buyer.id).orElseThrow();
        // Reservou 160*10=1600, executou a 140*10=1400, excesso=200
        assertEquals(0, new BigDecimal("8600.00").compareTo(buyerWallet.balanceBrl));
    }

    // === Helper de invariantes ===

    private void assertFinancialInvariants(BigDecimal expectedTotalBrl, BigDecimal expectedTotalVibranium) {
        BigDecimal actualTotalBrl = BigDecimal.ZERO;
        BigDecimal actualTotalVibranium = BigDecimal.ZERO;

        for (UUID userId : testUserIds) {
            Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(
                    () -> new AssertionError("Wallet nao encontrada para userId: " + userId));

            // Nao-negatividade de todos os campos
            assertTrue(wallet.balanceBrl.signum() >= 0,
                    "balanceBrl negativo para user " + userId + ": " + wallet.balanceBrl);
            assertTrue(wallet.balanceVibranium.signum() >= 0,
                    "balanceVibranium negativo para user " + userId + ": " + wallet.balanceVibranium);
            assertTrue(wallet.reservedBrl.signum() >= 0,
                    "reservedBrl negativo para user " + userId + ": " + wallet.reservedBrl);
            assertTrue(wallet.reservedVibranium.signum() >= 0,
                    "reservedVibranium negativo para user " + userId + ": " + wallet.reservedVibranium);

            actualTotalBrl = actualTotalBrl.add(wallet.balanceBrl).add(wallet.reservedBrl);
            actualTotalVibranium = actualTotalVibranium.add(wallet.balanceVibranium).add(wallet.reservedVibranium);
        }

        assertEquals(0, expectedTotalBrl.compareTo(actualTotalBrl),
                "Conservacao de BRL violada. Esperado: " + expectedTotalBrl + ", Actual: " + actualTotalBrl);
        assertEquals(0, expectedTotalVibranium.compareTo(actualTotalVibranium),
                "Conservacao de Vibranium violada. Esperado: " + expectedTotalVibranium + ", Actual: " + actualTotalVibranium);
    }

    // === Helper de setup ===

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

        testUserIds.add(user.id);
        return user;
    }
}
