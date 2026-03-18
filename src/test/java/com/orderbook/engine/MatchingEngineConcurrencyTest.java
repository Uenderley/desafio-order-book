package com.orderbook.engine;

import com.orderbook.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineConcurrencyTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    @Test
    void deveCasarOrdensCorretamenteComSubmissoesConcorrentes() throws Exception {
        // 50 sells pre-inseridos no book
        for (int i = 0; i < 50; i++) {
            engine.submitOrder(createOrder(OrderSide.SELL, "150.00", "1.00000000"));
        }

        // 100 buys submetidos concorrentemente
        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch gate = new CountDownLatch(1);
        ConcurrentLinkedQueue<MatchResult> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    gate.await();
                    Order buy = createOrder(OrderSide.BUY, "150.00", "1.00000000");
                    MatchResult result = engine.submitOrder(buy);
                    results.add(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        gate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(100, results.size());

        long totalTrades = results.stream()
                .mapToLong(r -> r.trades().size())
                .sum();

        // Exatamente 50 trades (50 sells casam com 50 dos 100 buys)
        assertEquals(50, totalTrades);
        // 50 buys restam no book (sem match)
        assertEquals(50, engine.getOrderCount());
    }

    @Test
    void deveCasarComSubmissoesMistasSimultaneas() throws Exception {
        int pairsCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(pairsCount * 2);
        CountDownLatch gate = new CountDownLatch(1);
        ConcurrentLinkedQueue<MatchResult> results = new ConcurrentLinkedQueue<>();

        // 50 buys + 50 sells simultaneos, todos ao mesmo preco
        for (int i = 0; i < pairsCount; i++) {
            executor.submit(() -> {
                try {
                    gate.await();
                    results.add(engine.submitOrder(createOrder(OrderSide.BUY, "150.00", "1.00000000")));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            executor.submit(() -> {
                try {
                    gate.await();
                    results.add(engine.submitOrder(createOrder(OrderSide.SELL, "150.00", "1.00000000")));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        gate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(pairsCount * 2, results.size());

        long totalTrades = results.stream()
                .mapToLong(r -> r.trades().size())
                .sum();

        // Cada trade aparece no resultado de quem fez match
        // Todas as ordens devem ser casadas: 50 trades
        assertEquals(pairsCount, totalTrades);
        assertEquals(0, engine.getOrderCount());
    }

    @Test
    void naoDevePerderOrdensComConcorrencia() throws Exception {
        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch gate = new CountDownLatch(1);
        ConcurrentLinkedQueue<MatchResult> results = new ConcurrentLinkedQueue<>();

        // 100 buys simultaneos sem nenhum sell — nenhum match
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    gate.await();
                    results.add(engine.submitOrder(createOrder(OrderSide.BUY, "150.00", "1.00000000")));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        gate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(100, results.size());

        long totalTrades = results.stream()
                .mapToLong(r -> r.trades().size())
                .sum();

        assertEquals(0, totalTrades);
        assertEquals(100, engine.getOrderCount());
    }

    @Test
    void deveManterConsistenciaComCancelamentosConcorrentes() throws Exception {
        int totalOrders = 100;
        Order[] orders = new Order[totalOrders];

        // Pre-insere 100 sells
        for (int i = 0; i < totalOrders; i++) {
            orders[i] = createOrder(OrderSide.SELL, "150.00", "1.00000000");
            engine.submitOrder(orders[i]);
        }
        assertEquals(totalOrders, engine.getOrderCount());

        // Cancela metade concorrentemente
        int cancels = totalOrders / 2;
        ExecutorService executor = Executors.newFixedThreadPool(cancels);
        CountDownLatch gate = new CountDownLatch(1);
        ConcurrentLinkedQueue<Boolean> cancelResults = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < cancels; i++) {
            final Order order = orders[i];
            executor.submit(() -> {
                try {
                    gate.await();
                    cancelResults.add(engine.removeOrder(order));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        gate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(cancels, cancelResults.size());
        long successfulCancels = cancelResults.stream().filter(r -> r).count();

        // orderCount + successfulCancels = totalOrders
        assertEquals(totalOrders, engine.getOrderCount() + successfulCancels);
    }

    // === Helper ===

    private Order createOrder(OrderSide side, String price, String quantity) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.user = new User();
        order.user.id = UUID.randomUUID();
        order.side = side;
        order.price = new BigDecimal(price);
        order.quantity = new BigDecimal(quantity);
        order.remaining = new BigDecimal(quantity);
        order.status = OrderStatus.NEW;
        order.createdAt = Instant.now();
        return order;
    }
}
