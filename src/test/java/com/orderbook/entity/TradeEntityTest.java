package com.orderbook.entity;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TradeEntityTest {

    @Inject
    EntityManager em;

    @Test
    @Transactional
    void devePersistirTradeEntreOrdens() {
        User buyer = createUser("buyer-trade@example.com");
        User seller = createUser("seller-trade@example.com");

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        Trade trade = new Trade();
        trade.buyOrder = buyOrder;
        trade.sellOrder = sellOrder;
        trade.price = new BigDecimal("150.00");
        trade.quantity = new BigDecimal("10.00000000");

        em.persist(trade);
        em.flush();

        assertNotNull(trade.id);
        assertNotNull(trade.executedAt);
        assertEquals(0, new BigDecimal("150.00").compareTo(trade.price));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(trade.quantity));
    }

    @Test
    @Transactional
    void naoDevePermitirTradesDuplicadosEntreMemasOrdens() {
        User buyer = createUser("buyer-dup@example.com");
        User seller = createUser("seller-dup@example.com");

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        Trade trade1 = new Trade();
        trade1.buyOrder = buyOrder;
        trade1.sellOrder = sellOrder;
        trade1.price = new BigDecimal("150.00");
        trade1.quantity = new BigDecimal("5.00000000");
        em.persist(trade1);
        em.flush();

        Trade trade2 = new Trade();
        trade2.buyOrder = buyOrder;
        trade2.sellOrder = sellOrder;
        trade2.price = new BigDecimal("150.00");
        trade2.quantity = new BigDecimal("5.00000000");

        assertThrows(Exception.class, () -> {
            em.persist(trade2);
            em.flush();
        });
    }

    private User createUser(String email) {
        User user = new User();
        user.name = "Test User";
        user.email = email;
        em.persist(user);
        return user;
    }

    private Order createOrder(User user, OrderSide side, String price, String quantity) {
        Order order = new Order();
        order.user = user;
        order.side = side;
        order.price = new BigDecimal(price);
        order.quantity = new BigDecimal(quantity);
        order.remaining = new BigDecimal(quantity);
        order.status = OrderStatus.NEW;
        em.persist(order);
        return order;
    }
}
