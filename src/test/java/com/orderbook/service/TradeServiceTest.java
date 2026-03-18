package com.orderbook.service;

import com.orderbook.engine.TradeResult;
import com.orderbook.entity.*;
import com.orderbook.repository.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TradeServiceTest {

    @Inject
    TradeService tradeService;

    @Inject
    UserRepository userRepository;

    @Inject
    WalletRepository walletRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    TradeRepository tradeRepository;

    @Inject
    TransactionHistoryRepository txnRepository;

    @Inject
    WalletService walletService;

    @Test
    @Transactional
    void deveExecutarTradeEPersistir() {
        User buyer = createUserWithWallet("trade-svc-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("trade-svc-seller@example.com", "0.00", "50.00000000");

        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        simulateEngineMatch(buyOrder, sellOrder, "10.00000000");
        TradeResult tr = buildTradeResult(buyOrder, sellOrder, "150.00", "10.00000000");
        tradeService.executeTrade(tr);

        List<Trade> trades = tradeRepository.findByOrderId(buyOrder.id);
        assertEquals(1, trades.size());
        assertEquals(0, new BigDecimal("150.00").compareTo(trades.get(0).price));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(trades.get(0).quantity));
    }

    @Test
    @Transactional
    void deveAtualizarStatusDasOrdensParaFilled() {
        User buyer = createUserWithWallet("filled-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("filled-seller@example.com", "0.00", "50.00000000");

        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        simulateEngineMatch(buyOrder, sellOrder, "10.00000000");
        tradeService.executeTrade(buildTradeResult(buyOrder, sellOrder, "150.00", "10.00000000"));

        assertEquals(OrderStatus.FILLED, buyOrder.status);
        assertEquals(0, BigDecimal.ZERO.compareTo(buyOrder.remaining));
        assertEquals(OrderStatus.FILLED, sellOrder.status);
        assertEquals(0, BigDecimal.ZERO.compareTo(sellOrder.remaining));
    }

    @Test
    @Transactional
    void deveAtualizarStatusParaPartiallyFilled() {
        User buyer = createUserWithWallet("partial-svc-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("partial-svc-seller@example.com", "0.00", "50.00000000");

        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("3.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "3.00000000");

        simulateEngineMatch(buyOrder, sellOrder, "3.00000000");
        tradeService.executeTrade(buildTradeResult(buyOrder, sellOrder, "150.00", "3.00000000"));

        assertEquals(OrderStatus.PARTIALLY_FILLED, buyOrder.status);
        assertEquals(0, new BigDecimal("7.00000000").compareTo(buyOrder.remaining));
        assertEquals(OrderStatus.FILLED, sellOrder.status);
    }

    @Test
    @Transactional
    void deveTransferirSaldosViaWalletService() {
        User buyer = createUserWithWallet("settle-svc-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("settle-svc-seller@example.com", "0.00", "50.00000000");

        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("155.00"), new BigDecimal("10.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "155.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        simulateEngineMatch(buyOrder, sellOrder, "10.00000000");
        tradeService.executeTrade(buildTradeResult(buyOrder, sellOrder, "150.00", "10.00000000"));

        Wallet buyerWallet = walletRepository.findByUserId(buyer.id).orElseThrow();
        Wallet sellerWallet = walletRepository.findByUserId(seller.id).orElseThrow();

        assertEquals(0, new BigDecimal("8500.00").compareTo(buyerWallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(buyerWallet.reservedBrl));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(buyerWallet.balanceVibranium));

        assertEquals(0, new BigDecimal("1500.00").compareTo(sellerWallet.balanceBrl));
        assertEquals(0, BigDecimal.ZERO.compareTo(sellerWallet.reservedVibranium));
    }

    @Test
    @Transactional
    void deveCriarTransactionHistoryParaAmbosOsLados() {
        User buyer = createUserWithWallet("txn-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("txn-seller@example.com", "0.00", "50.00000000");

        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        simulateEngineMatch(buyOrder, sellOrder, "10.00000000");
        tradeService.executeTrade(buildTradeResult(buyOrder, sellOrder, "150.00", "10.00000000"));

        List<TransactionHistory> buyerTxns = txnRepository.findByUserId(buyer.id, 0, 20);
        List<TransactionHistory> sellerTxns = txnRepository.findByUserId(seller.id, 0, 20);

        assertEquals(1, buyerTxns.size());
        assertEquals(OrderSide.BUY, buyerTxns.get(0).type);
        assertEquals(0, new BigDecimal("1500.00").compareTo(buyerTxns.get(0).totalValue));

        assertEquals(1, sellerTxns.size());
        assertEquals(OrderSide.SELL, sellerTxns.get(0).type);
    }

    @Test
    @Transactional
    void deveListarTradesPaginado() {
        User buyer = createUserWithWallet("list-trades-buyer@example.com", "10000.00", "0.00000000");
        User seller = createUserWithWallet("list-trades-seller@example.com", "0.00", "50.00000000");

        walletService.reserveBalance(buyer.id, OrderSide.BUY, new BigDecimal("150.00"), new BigDecimal("10.00000000"));
        walletService.reserveBalance(seller.id, OrderSide.SELL, new BigDecimal("150.00"), new BigDecimal("10.00000000"));

        Order buyOrder = createOrder(buyer, OrderSide.BUY, "150.00", "10.00000000");
        Order sellOrder = createOrder(seller, OrderSide.SELL, "150.00", "10.00000000");

        simulateEngineMatch(buyOrder, sellOrder, "10.00000000");
        tradeService.executeTrade(buildTradeResult(buyOrder, sellOrder, "150.00", "10.00000000"));

        List<Trade> trades = tradeService.listTrades(0, 20);
        assertFalse(trades.isEmpty());
    }

    // === Helpers ===

    private void simulateEngineMatch(Order buyOrder, Order sellOrder, String matchQty) {
        BigDecimal qty = new BigDecimal(matchQty);
        buyOrder.remaining = buyOrder.remaining.subtract(qty);
        sellOrder.remaining = sellOrder.remaining.subtract(qty);
    }

    private TradeResult buildTradeResult(Order buyOrder, Order sellOrder, String price, String qty) {
        return new TradeResult(buyOrder.id, sellOrder.id, buyOrder, sellOrder, new BigDecimal(price), new BigDecimal(qty));
    }

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
