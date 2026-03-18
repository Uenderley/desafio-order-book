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
class TradeRepositoryTest {

    @Inject
    TradeRepository tradeRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    void deveBuscarTradesPorOrderId() {
        User buyer = createUser("buyer-trade-repo@example.com");
        User seller = createUser("seller-trade-repo@example.com");

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        Trade trade = new Trade();
        trade.buyOrder = buyOrder;
        trade.sellOrder = sellOrder;
        trade.price = new BigDecimal("150.00");
        trade.quantity = new BigDecimal("10.00000000");
        tradeRepository.persist(trade);

        List<Trade> buyTrades = tradeRepository.findByOrderId(buyOrder.id);
        List<Trade> sellTrades = tradeRepository.findByOrderId(sellOrder.id);

        assertEquals(1, buyTrades.size());
        assertEquals(1, sellTrades.size());
    }

    @Test
    @Transactional
    void deveListarTradesOrdenadosPorExecutedAtDesc() {
        User buyer = createUser("buyer-list@example.com");
        User seller = createUser("seller-list@example.com");

        Order buyOrder1 = createOrder(buyer, OrderSide.BUY, "150.00", "5.00000000");
        Order sellOrder1 = createOrder(seller, OrderSide.SELL, "150.00", "5.00000000");

        Trade trade1 = new Trade();
        trade1.buyOrder = buyOrder1;
        trade1.sellOrder = sellOrder1;
        trade1.price = new BigDecimal("150.00");
        trade1.quantity = new BigDecimal("5.00000000");
        tradeRepository.persist(trade1);

        Order buyOrder2 = createOrder(buyer, OrderSide.BUY, "152.00", "3.00000000");
        Order sellOrder2 = createOrder(seller, OrderSide.SELL, "152.00", "3.00000000");

        Trade trade2 = new Trade();
        trade2.buyOrder = buyOrder2;
        trade2.sellOrder = sellOrder2;
        trade2.price = new BigDecimal("152.00");
        trade2.quantity = new BigDecimal("3.00000000");
        tradeRepository.persist(trade2);

        List<Trade> trades = tradeRepository.listRecent(0, 20);

        assertTrue(trades.size() >= 2);
        // Mais recentes primeiro
        for (int i = 1; i < trades.size(); i++) {
            assertTrue(trades.get(i).executedAt.compareTo(trades.get(i - 1).executedAt) <= 0);
        }
    }

    private User createUser(String email) {
        User user = new User();
        user.name = "Test User";
        user.email = email;
        userRepository.persist(user);
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
        orderRepository.persist(order);
        return order;
    }
}
