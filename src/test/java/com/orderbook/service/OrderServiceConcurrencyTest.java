package com.orderbook.service;

import com.orderbook.engine.MatchingEngine;
import com.orderbook.repository.WalletRepository;
import com.orderbook.entity.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OrderServiceConcurrencyTest {

    @Inject
    MatchingEngine matchingEngine;

    @Inject
    WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        matchingEngine.clear();
    }

    @Test
    void cenario7_100ComprasE100VendasSimultaneas() throws Exception {
        int pairs = 100;
        List<String> buyerIds = new ArrayList<>();
        List<String> sellerIds = new ArrayList<>();

        // Cria 100 buyers e 100 sellers via HTTP
        for (int i = 0; i < pairs; i++) {
            buyerIds.add(createUser("c7-buyer-" + i, "100000.00", "0.0"));
            sellerIds.add(createUser("c7-seller-" + i, "0.0", "1000.0"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(pairs * 2);
        CountDownLatch gate = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> statusCodes = new ConcurrentLinkedQueue<>();

        // 100 sells + 100 buys simultaneos, todos a R$150.00 qty=1
        for (int i = 0; i < pairs; i++) {
            final String sellerId = sellerIds.get(i);
            executor.submit(() -> {
                try {
                    gate.await();
                    int status = submitOrder(sellerId, "SELL", "150.00", "1.0");
                    statusCodes.add(status);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            final String buyerId = buyerIds.get(i);
            executor.submit(() -> {
                try {
                    gate.await();
                    int status = submitOrder(buyerId, "BUY", "150.00", "1.0");
                    statusCodes.add(status);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        gate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));

        assertEquals(pairs * 2, statusCodes.size());

        long accepted = statusCodes.stream().filter(s -> s == 201).count();
        long conflicts = statusCodes.stream().filter(s -> s == 500 || s == 409).count();

        // Todas as respostas devem ser 201 (sucesso) ou 500/409 (conflito de concorrencia)
        assertEquals(pairs * 2, accepted + conflicts,
                "Todas as respostas devem ser 201 ou 500/409. Status codes: " + statusCodes);

        // A grande maioria deve ter sucesso (pelo menos 90%)
        assertTrue(accepted >= pairs * 2 * 0.9,
                "Pelo menos 90% das ordens devem ser aceitas: " + accepted + "/" + (pairs * 2));

        // Nenhum saldo negativo — invariante critica
        assertNoNegativeBalances(buyerIds);
        assertNoNegativeBalances(sellerIds);
    }

    @Test
    void cenario7_concorrenciaComPrecosVariados() throws Exception {
        int count = 50;
        List<String> buyerIds = new ArrayList<>();
        List<String> sellerIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            buyerIds.add(createUser("c7-varied-buyer-" + i, "100000.00", "0.0"));
            sellerIds.add(createUser("c7-varied-seller-" + i, "0.0", "1000.0"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(count * 2);
        CountDownLatch gate = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> statusCodes = new ConcurrentLinkedQueue<>();

        // Sells de 140 a 160, buys de 145 a 165 — overlap parcial
        for (int i = 0; i < count; i++) {
            final String sellerId = sellerIds.get(i);
            final String sellPrice = String.valueOf(140 + i * 0.4);
            executor.submit(() -> {
                try {
                    gate.await();
                    statusCodes.add(submitOrder(sellerId, "SELL", sellPrice, "1.0"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            final String buyerId = buyerIds.get(i);
            final String buyPrice = String.valueOf(145 + i * 0.4);
            executor.submit(() -> {
                try {
                    gate.await();
                    statusCodes.add(submitOrder(buyerId, "BUY", buyPrice, "1.0"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        gate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));

        assertEquals(count * 2, statusCodes.size());

        long accepted = statusCodes.stream().filter(s -> s == 201).count();
        long conflicts = statusCodes.stream().filter(s -> s == 500 || s == 409).count();

        assertEquals(count * 2, accepted + conflicts,
                "Todas as respostas devem ser 201 ou 500/409. Status codes: " + statusCodes);

        assertTrue(accepted >= count * 2 * 0.9,
                "Pelo menos 90% das ordens devem ser aceitas: " + accepted + "/" + (count * 2));

        // Nenhum saldo negativo — invariante critica
        assertNoNegativeBalances(buyerIds);
        assertNoNegativeBalances(sellerIds);
    }

    @Test
    void cenario7_mesmoUsuarioNaoFicaComSaldoNegativo() throws Exception {
        // 1 buyer com saldo limitado (500 BRL), tenta 20 compras simultaneas de 1 Vibranium a R$100
        // Maximo possivel: 5 ordens aceitas (500/100=5)
        String buyerId = createUser("c7-limited-buyer", "500.00", "0.0");

        // 1 seller com muito Vibranium
        String sellerId = createUser("c7-limited-seller", "0.0", "1000.0");

        // Seller coloca 20 sells no book primeiro
        for (int i = 0; i < 20; i++) {
            submitOrder(sellerId, "SELL", "100.00", "1.0");
        }

        int attempts = 20;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch gate = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> statusCodes = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                try {
                    gate.await();
                    statusCodes.add(submitOrder(buyerId, "BUY", "100.00", "1.0"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        gate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(attempts, statusCodes.size());

        long accepted = statusCodes.stream().filter(s -> s == 201).count();
        long rejected = statusCodes.stream().filter(s -> s == 400).count();
        long conflicts = statusCodes.stream().filter(s -> s == 500 || s == 409).count();

        // Deve aceitar no maximo 5 (500 BRL / 100 BRL cada)
        assertTrue(accepted <= 5, "Nao deve aceitar mais que 5 ordens com 500 BRL: aceitas=" + accepted);
        // Respostas validas: 201 (aceita), 400 (saldo insuficiente), 500/409 (conflito optimistic lock)
        assertEquals(attempts, accepted + rejected + conflicts,
                "Todas as respostas devem ser 201, 400 ou 500/409. Status codes: " + statusCodes);

        // Saldo nunca negativo — invariante critica
        assertNoNegativeBalances(List.of(buyerId));
        assertNoNegativeBalances(List.of(sellerId));
    }

    // === Helpers ===

    private String createUser(String prefix, String brl, String vibranium) {
        return given()
                .contentType("application/json")
                .body("""
                    {"name": "Concurrent User", "email": "%s-%s@example.com",
                     "initialBalanceBrl": %s, "initialBalanceVibranium": %s}
                    """.formatted(prefix, UUID.randomUUID(), brl, vibranium))
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private int submitOrder(String userId, String side, String price, String quantity) {
        return given()
                .contentType("application/json")
                .header("X-User-Id", userId)
                .body("""
                    {"side": "%s", "price": %s, "quantity": %s}
                    """.formatted(side, price, quantity))
                .when()
                .post("/api/orders")
                .then()
                .extract().statusCode();
    }

    private void assertNoNegativeBalances(List<String> userIds) {
        for (String userIdStr : userIds) {
            UUID userId = UUID.fromString(userIdStr);
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet != null) {
                assertTrue(wallet.balanceBrl.signum() >= 0,
                        "balanceBrl negativo para user " + userId + ": " + wallet.balanceBrl);
                assertTrue(wallet.balanceVibranium.signum() >= 0,
                        "balanceVibranium negativo para user " + userId + ": " + wallet.balanceVibranium);
                assertTrue(wallet.reservedBrl.signum() >= 0,
                        "reservedBrl negativo para user " + userId + ": " + wallet.reservedBrl);
                assertTrue(wallet.reservedVibranium.signum() >= 0,
                        "reservedVibranium negativo para user " + userId + ": " + wallet.reservedVibranium);
            }
        }
    }
}
