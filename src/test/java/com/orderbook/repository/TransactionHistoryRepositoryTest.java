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
class TransactionHistoryRepositoryTest {

    @Inject
    TransactionHistoryRepository txnRepository;

    @Inject
    TradeRepository tradeRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    void deveBuscarHistoricoPorUserIdPaginado() {
        User buyer = createUser("buyer-txn-repo@example.com");
        User seller = createUser("seller-txn-repo@example.com");

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        Trade trade = new Trade();
        trade.buyOrder = buyOrder;
        trade.sellOrder = sellOrder;
        trade.price = new BigDecimal("150.00");
        trade.quantity = new BigDecimal("10.00000000");
        tradeRepository.persist(trade);

        createTxn(buyer, trade, OrderSide.BUY, "150.00", "10.00000000", "1500.00");
        createTxn(seller, trade, OrderSide.SELL, "150.00", "10.00000000", "1500.00");

        List<TransactionHistory> buyerHistory = txnRepository.findByUserId(buyer.id, 0, 20);
        List<TransactionHistory> sellerHistory = txnRepository.findByUserId(seller.id, 0, 20);

        assertEquals(1, buyerHistory.size());
        assertEquals(1, sellerHistory.size());
        assertEquals(OrderSide.BUY, buyerHistory.get(0).type);
        assertEquals(OrderSide.SELL, sellerHistory.get(0).type);
    }

    @Test
    @Transactional
    void deveContarHistoricoPorUserId() {
        User user = createUser("count-txn@example.com");
        User other = createUser("other-txn@example.com");

        Order buyOrder = createOrder(user, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(other, OrderSide.SELL, "150.00", "10.00000000");

        Trade trade = new Trade();
        trade.buyOrder = buyOrder;
        trade.sellOrder = sellOrder;
        trade.price = new BigDecimal("150.00");
        trade.quantity = new BigDecimal("10.00000000");
        tradeRepository.persist(trade);

        createTxn(user, trade, OrderSide.BUY, "150.00", "10.00000000", "1500.00");
        createTxn(other, trade, OrderSide.SELL, "150.00", "10.00000000", "1500.00");

        long count = txnRepository.countByUserId(user.id);

        assertEquals(1, count);
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

    private void createTxn(User user, Trade trade, OrderSide type, String price, String qty, String total) {
        TransactionHistory txn = new TransactionHistory();
        txn.user = user;
        txn.trade = trade;
        txn.type = type;
        txn.price = new BigDecimal(price);
        txn.quantity = new BigDecimal(qty);
        txn.totalValue = new BigDecimal(total);
        txnRepository.persist(txn);
    }
}
