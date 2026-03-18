package com.orderbook.service;

import com.orderbook.engine.MatchResult;
import com.orderbook.engine.MatchingEngine;
import com.orderbook.engine.TradeResult;
import com.orderbook.entity.*;
import com.orderbook.exception.OrderNotFoundException;
import com.orderbook.repository.OrderRepository;
import com.orderbook.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    WalletService walletService;

    @Inject
    MatchingEngine matchingEngine;

    @Inject
    TradeService tradeService;

    public Order createOrder(UUID userId, OrderSide side, BigDecimal price, BigDecimal quantity) {
        validateOrderParams(price, quantity);

        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuario nao encontrado: " + userId);
        }

        walletService.reserveBalance(userId, side, price, quantity);

        Order order = new Order();
        order.user = user;
        order.side = side;
        order.price = price;
        order.quantity = quantity;
        order.remaining = quantity;
        order.status = OrderStatus.NEW;
        orderRepository.persist(order);
        orderRepository.flush();

        // Submete ao matching engine
        MatchResult matchResult = matchingEngine.submitOrder(order);

        // Persiste trades e faz settlement
        for (TradeResult trade : matchResult.trades()) {
            tradeService.executeTrade(trade);
        }

        return order;
    }

    public Order cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }

        if (!order.user.id.equals(userId)) {
            throw new IllegalArgumentException("Ordem nao pertence ao usuario");
        }

        if (!order.status.isOpen()) {
            throw new IllegalStateException("Ordem com status " + order.status + " nao pode ser cancelada");
        }

        matchingEngine.removeOrder(order);
        walletService.releaseBalance(userId, order);

        order.status = OrderStatus.CANCELLED;
        orderRepository.persist(order);

        return order;
    }

    public Order getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return order;
    }

    public List<Order> getOrdersByUser(UUID userId, int page, int size) {
        return orderRepository.find("user.id = ?1 ORDER BY createdAt DESC", userId)
                .page(page, size).list();
    }

    private void validateOrderParams(BigDecimal price, BigDecimal quantity) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preco deve ser maior que zero");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
    }
}
