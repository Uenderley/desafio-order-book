package com.orderbook.entity;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TransactionHistoryEntityTest {

    @Inject
    EntityManager em;

    @Test
    @Transactional
    void devePersistirHistoricoDeTransacao() {
        User buyer = createUser("buyer-txn@example.com");
        User seller = createUser("seller-txn@example.com");

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        Trade trade = new Trade();
        trade.buyOrder = buyOrder;
        trade.sellOrder = sellOrder;
        trade.price = new BigDecimal("150.00");
        trade.quantity = new BigDecimal("10.00000000");
        em.persist(trade);

        TransactionHistory txnBuyer = new TransactionHistory();
        txnBuyer.user = buyer;
        txnBuyer.trade = trade;
        txnBuyer.type = OrderSide.BUY;
        txnBuyer.price = new BigDecimal("150.00");
        txnBuyer.quantity = new BigDecimal("10.00000000");
        txnBuyer.totalValue = new BigDecimal("1500.00");
        em.persist(txnBuyer);

        TransactionHistory txnSeller = new TransactionHistory();
        txnSeller.user = seller;
        txnSeller.trade = trade;
        txnSeller.type = OrderSide.SELL;
        txnSeller.price = new BigDecimal("150.00");
        txnSeller.quantity = new BigDecimal("10.00000000");
        txnSeller.totalValue = new BigDecimal("1500.00");
        em.persist(txnSeller);

        em.flush();

        assertNotNull(txnBuyer.id);
        assertNotNull(txnSeller.id);
        assertNotNull(txnBuyer.createdAt);
        assertEquals(OrderSide.BUY, txnBuyer.type);
        assertEquals(OrderSide.SELL, txnSeller.type);
        assertEquals(0, new BigDecimal("1500.00").compareTo(txnBuyer.totalValue));
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
