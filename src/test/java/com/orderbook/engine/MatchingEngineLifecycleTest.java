package com.orderbook.engine;

import com.orderbook.entity.Order;
import com.orderbook.entity.OrderSide;
import com.orderbook.entity.OrderStatus;
import com.orderbook.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineLifecycleTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    // === accepting flag ===

    @Test
    void deveAceitarOrdensPorPadrao() {
        assertTrue(engine.isAccepting());
    }

    @Test
    void deveRejeitarOrdensAposStopAccepting() {
        engine.stopAcceptingOrders();
        assertFalse(engine.isAccepting());

        Order order = createOrder(OrderSide.BUY, "150.00", "10.00000000");
        assertThrows(IllegalStateException.class, () -> engine.submitOrder(order));
    }

    // === ready flag ===

    @Test
    void naoDeveEstarProntoAntesDaReconstrucao() {
        assertFalse(engine.isReady());
    }

    @Test
    void deveEstarProntoAposSetReady() {
        engine.setReady(true);
        assertTrue(engine.isReady());
    }

    // === runRecoveryMatching ===

    @Test
    void recoveryMatchingDeveCasarOrdensPendentes() {
        // Simula ordens carregadas do banco sem matching
        Order sell = createOrder(OrderSide.SELL, "150.00", "10.00000000");
        engine.insertWithoutMatching(sell);

        Order buy = createOrder(OrderSide.BUY, "150.00", "10.00000000");
        engine.insertWithoutMatching(buy);

        assertEquals(2, engine.getOrderCount());

        // Re-matching deve casar as duas
        var trades = engine.runRecoveryMatching();

        assertEquals(1, trades.size());
        assertEquals(0, engine.getOrderCount());
        assertEquals(0, new BigDecimal("150.00").compareTo(trades.get(0).price()));
    }

    @Test
    void recoveryMatchingDeveCasarMultiplasOrdens() {
        engine.insertWithoutMatching(createOrder(OrderSide.SELL, "150.00", "3.00000000"));
        engine.insertWithoutMatching(createOrder(OrderSide.SELL, "152.00", "4.00000000"));
        engine.insertWithoutMatching(createOrder(OrderSide.BUY, "155.00", "7.00000000"));

        var trades = engine.runRecoveryMatching();

        assertEquals(2, trades.size());
        assertEquals(0, engine.getOrderCount());
    }

    @Test
    void recoveryMatchingSemOrdensCompativeisNaoGeraTrades() {
        engine.insertWithoutMatching(createOrder(OrderSide.SELL, "160.00", "10.00000000"));
        engine.insertWithoutMatching(createOrder(OrderSide.BUY, "150.00", "10.00000000"));

        var trades = engine.runRecoveryMatching();

        assertTrue(trades.isEmpty());
        assertEquals(2, engine.getOrderCount());
    }

    // === drainInFlightOperations ===

    @Test
    void drainDeveRetornarQuandoNaoHaOperacoesEmAndamento() {
        assertDoesNotThrow(() -> engine.drainInFlightOperations(java.time.Duration.ofSeconds(1)));
    }

    // === Helpers ===

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
