package com.orderbook.infrastructure;

import com.orderbook.engine.MatchingEngine;
import com.orderbook.entity.*;
import com.orderbook.repository.OrderRepository;
import com.orderbook.repository.UserRepository;
import com.orderbook.repository.WalletRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OrderBookRecoveryTest {

    @Inject
    MatchingEngine matchingEngine;

    @Inject
    OrderRepository orderRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        matchingEngine.clear();
    }

    @Test
    void clearDeveResetarReadyFlag() {
        matchingEngine.setReady(true);
        assertTrue(matchingEngine.isReady());
        matchingEngine.clear();
        assertFalse(matchingEngine.isReady());
    }

    @Test
    @Transactional
    void findOpenOrdersDeveRetornarApenasOrdensAbertas() {
        User user = createUserWithWallet("recovery-open@example.com", "10000.00", "50.00000000");

        createOrderInDb(user, OrderSide.BUY, "150.00", "10.00000000", OrderStatus.NEW);
        createOrderInDb(user, OrderSide.SELL, "160.00", "5.00000000", OrderStatus.PARTIALLY_FILLED);
        createOrderInDb(user, OrderSide.BUY, "100.00", "1.00000000", OrderStatus.FILLED);
        createOrderInDb(user, OrderSide.SELL, "200.00", "1.00000000", OrderStatus.CANCELLED);

        List<Order> openOrders = orderRepository.findOpenOrders();

        // Deve conter pelo menos as 2 que criamos (pode ter de outros testes)
        long userOrders = openOrders.stream().filter(o -> o.user.id.equals(user.id)).count();
        assertEquals(2, userOrders);
        assertTrue(openOrders.stream().allMatch(o -> o.status.isOpen()));
    }

    @Test
    void insertWithoutMatchingERecoveryMatchingDevemFuncionar() {
        // Testa a mecanica de recovery isolada do banco
        matchingEngine.clear();

        // Simula ordens carregadas sem matching
        Order buy = createInMemoryOrder(OrderSide.BUY, "150.00", "10.00000000");
        Order sell = createInMemoryOrder(OrderSide.SELL, "150.00", "10.00000000");

        matchingEngine.insertWithoutMatching(buy);
        matchingEngine.insertWithoutMatching(sell);

        assertEquals(2, matchingEngine.getOrderCount());

        // Re-matching deve casar
        var trades = matchingEngine.runRecoveryMatching();

        assertEquals(1, trades.size());
        assertEquals(0, matchingEngine.getOrderCount());
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

    private void createOrderInDb(User user, OrderSide side, String price, String quantity, OrderStatus status) {
        Order order = new Order();
        order.user = user;
        order.side = side;
        order.price = new BigDecimal(price);
        order.quantity = new BigDecimal(quantity);
        order.remaining = new BigDecimal(quantity);
        order.status = status;
        orderRepository.persist(order);
    }

    private Order createInMemoryOrder(OrderSide side, String price, String quantity) {
        Order order = new Order();
        order.id = java.util.UUID.randomUUID();
        order.user = new User();
        order.user.id = java.util.UUID.randomUUID();
        order.side = side;
        order.price = new BigDecimal(price);
        order.quantity = new BigDecimal(quantity);
        order.remaining = new BigDecimal(quantity);
        order.status = OrderStatus.NEW;
        order.createdAt = java.time.Instant.now();
        return order;
    }
}
