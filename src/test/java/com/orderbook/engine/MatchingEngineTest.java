package com.orderbook.engine;

import com.orderbook.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    // === Sem match ===

    @Test
    void ordemDeCompraSemVendedoresDeveFicarNoBook() {
        Order buy = createOrder(OrderSide.BUY, "150.00", "10.00000000");

        MatchResult result = engine.submitOrder(buy);

        assertTrue(result.trades().isEmpty());
        assertEquals(1, engine.getOrderCount());
    }

    @Test
    void ordemDeVendaSemCompradoresDeveFicarNoBook() {
        Order sell = createOrder(OrderSide.SELL, "150.00", "10.00000000");

        MatchResult result = engine.submitOrder(sell);

        assertTrue(result.trades().isEmpty());
        assertEquals(1, engine.getOrderCount());
    }

    @Test
    void semMatchQuandoPrecoDeCompraMenorQuePrecoDeVenda() {
        Order sell = createOrder(OrderSide.SELL, "155.00", "10.00000000");
        engine.submitOrder(sell);

        Order buy = createOrder(OrderSide.BUY, "150.00", "10.00000000");
        MatchResult result = engine.submitOrder(buy);

        assertTrue(result.trades().isEmpty());
        assertEquals(2, engine.getOrderCount());
    }

    // === Match exato ===

    @Test
    void matchExatoDeveGerarUmTrade() {
        Order sell = createOrder(OrderSide.SELL, "150.00", "10.00000000");
        engine.submitOrder(sell);

        Order buy = createOrder(OrderSide.BUY, "150.00", "10.00000000");
        MatchResult result = engine.submitOrder(buy);

        assertEquals(1, result.trades().size());
        TradeResult trade = result.trades().get(0);
        assertEquals(0, new BigDecimal("150.00").compareTo(trade.price()));
        assertEquals(0, new BigDecimal("10.00000000").compareTo(trade.quantity()));
        assertEquals(0, engine.getOrderCount());
    }

    @Test
    void precoDeExecucaoDeveSerDoMaker() {
        // Seller (maker) coloca a R$148
        Order sell = createOrder(OrderSide.SELL, "148.00", "10.00000000");
        engine.submitOrder(sell);

        // Buyer (taker) chega a R$155 — executa a R$148 (preco do maker)
        Order buy = createOrder(OrderSide.BUY, "155.00", "10.00000000");
        MatchResult result = engine.submitOrder(buy);

        assertEquals(1, result.trades().size());
        assertEquals(0, new BigDecimal("148.00").compareTo(result.trades().get(0).price()));
    }

    // === Match parcial ===

    @Test
    void matchParcialDeveAtualizarRemaining() {
        Order sell = createOrder(OrderSide.SELL, "150.00", "3.00000000");
        engine.submitOrder(sell);

        Order buy = createOrder(OrderSide.BUY, "150.00", "10.00000000");
        MatchResult result = engine.submitOrder(buy);

        assertEquals(1, result.trades().size());
        assertEquals(0, new BigDecimal("3.00000000").compareTo(result.trades().get(0).quantity()));

        // Buyer fica no book com remaining=7
        assertEquals(0, new BigDecimal("7.00000000").compareTo(buy.remaining));
        assertEquals(1, engine.getOrderCount());
    }

    @Test
    void matchParcialDoSellerDeveAtualizarRemaining() {
        Order buy = createOrder(OrderSide.BUY, "150.00", "3.00000000");
        engine.submitOrder(buy);

        Order sell = createOrder(OrderSide.SELL, "150.00", "10.00000000");
        MatchResult result = engine.submitOrder(sell);

        assertEquals(1, result.trades().size());
        assertEquals(0, new BigDecimal("3.00000000").compareTo(result.trades().get(0).quantity()));
        assertEquals(0, new BigDecimal("7.00000000").compareTo(sell.remaining));
        assertEquals(1, engine.getOrderCount());
    }

    // === Multiplos matches ===

    @Test
    void deveGerarMultiplosTradesAtePreencherOrdem() {
        // 3 vendedores no book
        engine.submitOrder(createOrder(OrderSide.SELL, "150.00", "3.00000000"));
        engine.submitOrder(createOrder(OrderSide.SELL, "152.00", "4.00000000"));
        engine.submitOrder(createOrder(OrderSide.SELL, "155.00", "5.00000000"));

        // Comprador chega querendo 10 a R$155
        Order buy = createOrder(OrderSide.BUY, "155.00", "10.00000000");
        MatchResult result = engine.submitOrder(buy);

        assertEquals(3, result.trades().size());

        // Trade 1: 3 @ R$150 (maker)
        assertEquals(0, new BigDecimal("150.00").compareTo(result.trades().get(0).price()));
        assertEquals(0, new BigDecimal("3.00000000").compareTo(result.trades().get(0).quantity()));

        // Trade 2: 4 @ R$152 (maker)
        assertEquals(0, new BigDecimal("152.00").compareTo(result.trades().get(1).price()));
        assertEquals(0, new BigDecimal("4.00000000").compareTo(result.trades().get(1).quantity()));

        // Trade 3: 3 @ R$155 (maker) — parcial do ask de 5
        assertEquals(0, new BigDecimal("155.00").compareTo(result.trades().get(2).price()));
        assertEquals(0, new BigDecimal("3.00000000").compareTo(result.trades().get(2).quantity()));

        // Buyer preenchido, 1 ask restante com remaining=2
        assertEquals(0, BigDecimal.ZERO.compareTo(buy.remaining));
        assertEquals(1, engine.getOrderCount());
    }

    // === Price-Time Priority ===

    @Test
    void deveRespeitarPrioridadeDePrecoEmAsks() {
        // Venda mais cara primeiro, depois mais barata
        Order expensiveSell = createOrder(OrderSide.SELL, "155.00", "5.00000000");
        engine.submitOrder(expensiveSell);

        Order cheapSell = createOrder(OrderSide.SELL, "150.00", "5.00000000");
        engine.submitOrder(cheapSell);

        // Comprador deve casar com a mais barata primeiro
        Order buy = createOrder(OrderSide.BUY, "155.00", "5.00000000");
        MatchResult result = engine.submitOrder(buy);

        assertEquals(1, result.trades().size());
        assertEquals(0, new BigDecimal("150.00").compareTo(result.trades().get(0).price()));
    }

    @Test
    void deveRespeitarPrioridadeDePrecoEmBids() {
        // Compra mais barata primeiro, depois mais cara
        Order cheapBuy = createOrder(OrderSide.BUY, "148.00", "5.00000000");
        engine.submitOrder(cheapBuy);

        Order expensiveBuy = createOrder(OrderSide.BUY, "155.00", "5.00000000");
        engine.submitOrder(expensiveBuy);

        // Vendedor deve casar com o que paga mais primeiro
        Order sell = createOrder(OrderSide.SELL, "148.00", "5.00000000");
        MatchResult result = engine.submitOrder(sell);

        assertEquals(1, result.trades().size());
        assertEquals(0, new BigDecimal("155.00").compareTo(result.trades().get(0).price()));
    }

    @Test
    void deveRespeitarFifoNoMesmoPreco() {
        UUID firstSellerId = UUID.randomUUID();
        UUID secondSellerId = UUID.randomUUID();

        Order firstSell = createOrderWithUser(firstSellerId, OrderSide.SELL, "150.00", "5.00000000");
        engine.submitOrder(firstSell);

        Order secondSell = createOrderWithUser(secondSellerId, OrderSide.SELL, "150.00", "5.00000000");
        engine.submitOrder(secondSell);

        // Comprador deve casar com o primeiro vendedor (FIFO)
        Order buy = createOrder(OrderSide.BUY, "150.00", "5.00000000");
        MatchResult result = engine.submitOrder(buy);

        assertEquals(1, result.trades().size());
        assertEquals(firstSell.id, result.trades().get(0).sellOrderId());
    }

    // === insertWithoutMatching ===

    @Test
    void insertWithoutMatchingDeveAdicionarSemCasar() {
        Order sell = createOrder(OrderSide.SELL, "150.00", "10.00000000");
        engine.insertWithoutMatching(sell);

        Order buy = createOrder(OrderSide.BUY, "150.00", "10.00000000");
        engine.insertWithoutMatching(buy);

        // Ambas no book, sem match
        assertEquals(2, engine.getOrderCount());
    }

    // === removeOrder ===

    @Test
    void removeOrderDeveRetirarDoBook() {
        Order sell = createOrder(OrderSide.SELL, "150.00", "10.00000000");
        engine.submitOrder(sell);

        boolean removed = engine.removeOrder(sell);
        assertTrue(removed);
        assertEquals(0, engine.getOrderCount());
    }

    @Test
    void removeOrderInexistenteDeveRetornarFalse() {
        Order ghost = createOrder(OrderSide.SELL, "150.00", "10.00000000");
        // nao submetida ao engine

        boolean removed = engine.removeOrder(ghost);
        assertFalse(removed);
    }

    @Test
    void ordemRemovidaNaoDeveCasarComNovasOrdens() {
        Order sell = createOrder(OrderSide.SELL, "150.00", "10.00000000");
        engine.submitOrder(sell);
        engine.removeOrder(sell);

        Order buy = createOrder(OrderSide.BUY, "150.00", "10.00000000");
        MatchResult result = engine.submitOrder(buy);

        assertTrue(result.trades().isEmpty());
        assertEquals(1, engine.getOrderCount());
    }

    // === getOrderBook ===

    @Test
    void getOrderBookDeveRetornarSnapshotDoBook() {
        engine.submitOrder(createOrder(OrderSide.BUY, "150.00", "10.00000000"));
        engine.submitOrder(createOrder(OrderSide.BUY, "149.00", "5.00000000"));
        engine.submitOrder(createOrder(OrderSide.SELL, "155.00", "3.00000000"));
        engine.submitOrder(createOrder(OrderSide.SELL, "160.00", "7.00000000"));

        OrderBookSnapshot snapshot = engine.getOrderBook();

        assertEquals(2, snapshot.bids().size());
        assertEquals(2, snapshot.asks().size());

        // Bids: maior preco primeiro
        assertTrue(snapshot.bids().get(0).price().compareTo(snapshot.bids().get(1).price()) > 0);
        // Asks: menor preco primeiro
        assertTrue(snapshot.asks().get(0).price().compareTo(snapshot.asks().get(1).price()) < 0);
    }

    // === Helpers ===

    private Order createOrder(OrderSide side, String price, String quantity) {
        return createOrderWithUser(UUID.randomUUID(), side, price, quantity);
    }

    private Order createOrderWithUser(UUID userId, OrderSide side, String price, String quantity) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.user = new User();
        order.user.id = userId;
        order.side = side;
        order.price = new BigDecimal(price);
        order.quantity = new BigDecimal(quantity);
        order.remaining = new BigDecimal(quantity);
        order.status = OrderStatus.NEW;
        order.createdAt = Instant.now();
        return order;
    }
}
