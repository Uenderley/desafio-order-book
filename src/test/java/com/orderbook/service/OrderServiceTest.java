package com.orderbook.service;

import com.orderbook.entity.*;
import com.orderbook.exception.InsufficientBalanceException;
import com.orderbook.exception.OrderNotFoundException;
import com.orderbook.repository.OrderRepository;
import com.orderbook.repository.UserRepository;
import com.orderbook.repository.WalletRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OrderServiceTest {

    @Inject
    OrderService orderService;

    @Inject
    UserRepository userRepository;

    @Inject
    WalletRepository walletRepository;

    @Inject
    OrderRepository orderRepository;

    // === createOrder ===

    @Test
    @Transactional
    void deveCriarOrdemDeCompraComStatusNew() {
        User user = createUserWithWallet("create-buy@example.com", "10000.00", "0.00000000");

        Order order = orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        assertNotNull(order.id);
        assertEquals(OrderSide.BUY, order.side);
        assertEquals(OrderStatus.NEW, order.status);
        assertEquals(0, new BigDecimal("150.00").compareTo(order.price));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(order.quantity));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(order.remaining));
    }

    @Test
    @Transactional
    void deveCriarOrdemDeVendaComStatusNew() {
        User user = createUserWithWallet("create-sell@example.com", "0.00", "50.00000000");

        Order order = orderService.createOrder(user.id, OrderSide.SELL, new BigDecimal("155.00"), new BigDecimal("5.00000000"));

        assertNotNull(order.id);
        assertEquals(OrderSide.SELL, order.side);
        assertEquals(OrderStatus.NEW, order.status);
    }

    @Test
    @Transactional
    void deveCriarOrdemEReservarSaldoBrl() {
        User user = createUserWithWallet("reserve-on-create@example.com", "10000.00", "0.00000000");

        orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        assertEquals(0, new BigDecimal("8500.00").compareTo(wallet.balanceBrl));
        assertEquals(0, new BigDecimal("1500.00").compareTo(wallet.reservedBrl));
    }

    @Test
    @Transactional
    void deveCriarOrdemEReservarSaldoVibranium() {
        User user = createUserWithWallet("reserve-vib-create@example.com", "0.00", "50.00000000");

        orderService.createOrder(user.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        assertEquals(0, new BigDecimal("40.00000000").compareTo(wallet.balanceVibranium));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(wallet.reservedVibranium));
    }

    @Test
    @Transactional
    void deveRejeitarOrdemComSaldoInsuficiente() {
        User user = createUserWithWallet("insuf-create@example.com", "100.00", "0.00000000");

        assertThrows(InsufficientBalanceException.class, () ->
            orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"))
        );

        // Nenhuma ordem deve ter sido criada
        List<Order> orders = orderRepository.findByUserId(user.id);
        assertTrue(orders.isEmpty());
    }

    @Test
    @Transactional
    void deveRejeitarOrdemComPrecoZeroOuNegativo() {
        User user = createUserWithWallet("price-zero@example.com", "10000.00", "50.00000000");

        assertThrows(IllegalArgumentException.class, () ->
            orderService.createOrder(user.id, OrderSide.BUY, BigDecimal.ZERO, new BigDecimal("10.00000000"))
        );

        assertThrows(IllegalArgumentException.class, () ->
            orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("-1.00"), new BigDecimal("10.00000000"))
        );
    }

    @Test
    @Transactional
    void deveRejeitarOrdemComQuantidadeZeroOuNegativa() {
        User user = createUserWithWallet("qty-zero@example.com", "10000.00", "50.00000000");

        assertThrows(IllegalArgumentException.class, () ->
            orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), BigDecimal.ZERO)
        );

        assertThrows(IllegalArgumentException.class, () ->
            orderService.createOrder(user.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("-5.00000000"))
        );
    }

    // === cancelOrder ===

    @Test
    @Transactional
    void deveCancelarOrdemAbertaELiberarSaldoBrl() {
        User user = createUserWithWallet("cancel-buy@example.com", "10000.00", "0.00000000");
        Order order = orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order cancelled = orderService.cancelOrder(user.id, order.id);

        assertEquals(OrderStatus.CANCELLED, cancelled.status);

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        assertEquals(0, new BigDecimal("10000.00").compareTo(wallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.reservedBrl));
    }

    @Test
    @Transactional
    void deveCancelarOrdemAbertaELiberarSaldoVibranium() {
        User user = createUserWithWallet("cancel-sell@example.com", "0.00", "50.00000000");
        Order order = orderService.createOrder(user.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order cancelled = orderService.cancelOrder(user.id, order.id);

        assertEquals(OrderStatus.CANCELLED, cancelled.status);

        Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
        assertEquals(0, new BigDecimal("50.00000000").compareTo(wallet.balanceVibranium));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.reservedVibranium));
    }

    @Test
    @Transactional
    void naoDeveCancelarOrdemDeOutroUsuario() {
        User owner = createUserWithWallet("owner-cancel@example.com", "10000.00", "0.00000000");
        User other = createUserWithWallet("other-cancel@example.com", "10000.00", "0.00000000");
        Order order = orderService.createOrder(owner.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("5.00000000"));

        assertThrows(IllegalArgumentException.class, () ->
            orderService.cancelOrder(other.id, order.id)
        );
    }

    @Test
    @Transactional
    void naoDeveCancelarOrdemJaCancelada() {
        User user = createUserWithWallet("already-cancelled@example.com", "10000.00", "0.00000000");
        Order order = orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("5.00000000"));
        orderService.cancelOrder(user.id, order.id);

        assertThrows(IllegalStateException.class, () ->
            orderService.cancelOrder(user.id, order.id)
        );
    }

    @Test
    @Transactional
    void naoDeveCancelarOrdemFilled() {
        User user = createUserWithWallet("filled-cancel@example.com", "10000.00", "0.00000000");
        Order order = orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("5.00000000"));

        // Simula ordem preenchida
        order.status = OrderStatus.FILLED;
        order.remaining = BigDecimal.ZERO;
        orderRepository.persist(order);

        assertThrows(IllegalStateException.class, () ->
            orderService.cancelOrder(user.id, order.id)
        );
    }

    @Test
    @Transactional
    void deveLancarExcecaoQuandoOrdemNaoExiste() {
        User user = createUserWithWallet("not-found@example.com", "10000.00", "0.00000000");

        assertThrows(OrderNotFoundException.class, () ->
            orderService.cancelOrder(user.id, UUID.randomUUID())
        );
    }

    // === getOrder / getOrdersByUser ===

    @Test
    @Transactional
    void deveBuscarOrdemPorId() {
        User user = createUserWithWallet("get-order@example.com", "10000.00", "0.00000000");
        Order created = orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order found = orderService.getOrder(created.id);

        assertEquals(created.id, found.id);
        assertEquals(OrderSide.BUY, found.side);
    }

    @Test
    @Transactional
    void deveLancarExcecaoQuandoOrdemNaoExisteNoGet() {
        assertThrows(OrderNotFoundException.class, () ->
            orderService.getOrder(UUID.randomUUID())
        );
    }

    @Test
    @Transactional
    void deveListarOrdensPorUsuario() {
        User user = createUserWithWallet("list-orders@example.com", "10000.00", "50.00000000");
        orderService.createOrder(user.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("5.00000000"));
        orderService.createOrder(user.id, OrderSide.SELL, new BigDecimal("155.00"), new BigDecimal("3.00000000"));

        List<Order> orders = orderService.getOrdersByUser(user.id, 0, 20);

        assertEquals(2, orders.size());
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
}
