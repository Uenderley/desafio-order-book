package com.orderbook.repository;

import com.orderbook.entity.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OrderRepositoryTest {

    @Inject
    OrderRepository orderRepository;

    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    void deveBuscarOrdensPorUserId() {
        User user = createUser("orders-by-user@example.com");
        createOrder(user, OrderSide.BUY, "150.00", "10.00000000", OrderStatus.NEW);
        createOrder(user, OrderSide.SELL, "155.00", "5.00000000", OrderStatus.NEW);

        List<Order> orders = orderRepository.findByUserId(user.id);

        assertEquals(2, orders.size());
    }

    @Test
    @Transactional
    void deveBuscarOrdensAbertasParaRecovery() {
        long openBefore = orderRepository.findOpenOrders().size();

        User user = createUser("open-orders@example.com");
        createOrder(user, OrderSide.BUY, "150.00", "10.00000000", OrderStatus.NEW);
        createOrder(user, OrderSide.BUY, "149.00", "5.00000000", OrderStatus.PARTIALLY_FILLED);
        createOrder(user, OrderSide.SELL, "155.00", "3.00000000", OrderStatus.FILLED);
        createOrder(user, OrderSide.SELL, "160.00", "2.00000000", OrderStatus.CANCELLED);

        List<Order> openOrders = orderRepository.findOpenOrders();

        assertEquals(openBefore + 2, openOrders.size());
        assertTrue(openOrders.stream().allMatch(o -> o.status.isOpen()));
    }

    @Test
    @Transactional
    void ordensAbertasDevemEstarOrdenadasPorCreatedAt() {
        User user = createUser("ordered-open@example.com");
        Order first = createOrder(user, OrderSide.BUY, "150.00", "10.00000000", OrderStatus.NEW);
        Order second = createOrder(user, OrderSide.SELL, "155.00", "5.00000000", OrderStatus.NEW);

        List<Order> openOrders = orderRepository.findOpenOrders();

        assertTrue(openOrders.size() >= 2);
        // Verifica que estao ordenadas por created_at ASC
        for (int i = 1; i < openOrders.size(); i++) {
            assertTrue(openOrders.get(i).createdAt.compareTo(openOrders.get(i - 1).createdAt) >= 0);
        }
    }

    private User createUser(String email) {
        User user = new User();
        user.name = "Test User";
        user.email = email;
        userRepository.persist(user);
        return user;
    }

    private Order createOrder(User user, OrderSide side, String price, String quantity, OrderStatus status) {
        Order order = new Order();
        order.user = user;
        order.side = side;
        order.price = new BigDecimal(price);
        order.quantity = new BigDecimal(quantity);
        order.remaining = new BigDecimal(quantity);
        order.status = status;
        orderRepository.persist(order);
        return order;
    }
}
