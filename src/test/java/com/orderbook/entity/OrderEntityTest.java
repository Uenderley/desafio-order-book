package com.orderbook.entity;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OrderEntityTest {

    @Inject
    EntityManager em;

    @Test
    @Transactional
    void devePersistirOrdemDeCompra() {
        User user = createUser("buyer-order@example.com");

        Order order = new Order();
        order.user = user;
        order.side = OrderSide.BUY;
        order.price = new BigDecimal("150.00");
        order.quantity = new BigDecimal("10.00000000");
        order.remaining = new BigDecimal("10.00000000");
        order.status = OrderStatus.NEW;

        em.persist(order);
        em.flush();

        assertNotNull(order.id);
        assertNotNull(order.createdAt);
        assertNotNull(order.updatedAt);
        assertEquals(OrderSide.BUY, order.side);
        assertEquals(OrderStatus.NEW, order.status);
    }

    @Test
    @Transactional
    void devePersistirOrdemDeVenda() {
        User user = createUser("seller-order@example.com");

        Order order = new Order();
        order.user = user;
        order.side = OrderSide.SELL;
        order.price = new BigDecimal("155.00");
        order.quantity = new BigDecimal("5.00000000");
        order.remaining = new BigDecimal("5.00000000");
        order.status = OrderStatus.NEW;

        em.persist(order);
        em.flush();

        Order found = em.find(Order.class, order.id);
        assertEquals(OrderSide.SELL, found.side);
        assertEquals(0, new BigDecimal("155.00").compareTo(found.price));
        assertEquals(0, new BigDecimal("5.00000000").compareTo(found.remaining));
    }

    @Test
    @Transactional
    void deveAtualizarStatusParaPartiallyFilled() {
        User user = createUser("partial-order@example.com");

        Order order = new Order();
        order.user = user;
        order.side = OrderSide.BUY;
        order.price = new BigDecimal("150.00");
        order.quantity = new BigDecimal("10.00000000");
        order.remaining = new BigDecimal("10.00000000");
        order.status = OrderStatus.NEW;
        em.persist(order);
        em.flush();

        order.remaining = new BigDecimal("7.00000000");
        order.status = OrderStatus.PARTIALLY_FILLED;
        em.merge(order);
        em.flush();

        Order found = em.find(Order.class, order.id);
        assertEquals(OrderStatus.PARTIALLY_FILLED, found.status);
        assertEquals(0, new BigDecimal("7.00000000").compareTo(found.remaining));
    }

    private User createUser(String email) {
        User user = new User();
        user.name = "Test User";
        user.email = email;
        em.persist(user);
        return user;
    }
}
