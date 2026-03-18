package com.orderbook.service;

import com.orderbook.engine.TradeResult;
import com.orderbook.entity.*;
import com.orderbook.repository.OrderRepository;
import com.orderbook.repository.TradeRepository;
import com.orderbook.repository.TransactionHistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class TradeService {

    @Inject
    TradeRepository tradeRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    TransactionHistoryRepository txnRepository;

    @Inject
    WalletService walletService;

    public void executeTrade(TradeResult tradeResult) {
        Order buyOrder = tradeResult.buyOrder();
        Order sellOrder = tradeResult.sellOrder();

        // Persiste trade
        Trade trade = new Trade();
        trade.buyOrder = buyOrder;
        trade.sellOrder = sellOrder;
        trade.price = tradeResult.price();
        trade.quantity = tradeResult.quantity();
        tradeRepository.persist(trade);

        // Atualiza status (remaining ja foi atualizado pelo MatchingEngine)
        buyOrder.status = buyOrder.remaining.signum() == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
        sellOrder.status = sellOrder.remaining.signum() == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;

        // Settlement de wallets
        walletService.settleTrade(buyOrder, sellOrder, tradeResult.price(), tradeResult.quantity());

        // Transaction history
        createTransactionHistory(buyOrder.user, trade, OrderSide.BUY, tradeResult.price(), tradeResult.quantity());
        createTransactionHistory(sellOrder.user, trade, OrderSide.SELL, tradeResult.price(), tradeResult.quantity());
    }

    public List<Trade> listTrades(int page, int size) {
        return tradeRepository.listRecent(page, size);
    }

    private void createTransactionHistory(User user, Trade trade, OrderSide type, BigDecimal price, BigDecimal quantity) {
        TransactionHistory txn = new TransactionHistory();
        txn.user = user;
        txn.trade = trade;
        txn.type = type;
        txn.price = price;
        txn.quantity = quantity;
        txn.totalValue = price.multiply(quantity);
        txnRepository.persist(txn);
    }
}
