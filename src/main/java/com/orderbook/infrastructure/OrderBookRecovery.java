package com.orderbook.infrastructure;

import com.orderbook.engine.MatchingEngine;
import com.orderbook.engine.TradeResult;
import com.orderbook.entity.Order;
import com.orderbook.repository.OrderRepository;
import com.orderbook.service.TradeService;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class OrderBookRecovery {

    @Inject
    MatchingEngine engine;

    @Inject
    OrderRepository orderRepository;

    @Inject
    TradeService tradeService;

    void onStartup(@Observes StartupEvent ev) {
        reconstruct();
    }

    @Transactional
    public void reconstruct() {
        Log.info("Iniciando reconstrucao do Order Book...");

        List<Order> openOrders = orderRepository.findOpenOrders();

        for (Order order : openOrders) {
            engine.insertWithoutMatching(order);
        }

        Log.infof("Order Book reconstruido: %d ordens carregadas", openOrders.size());

        // Re-matching para casar ordens que ficaram pendentes
        Log.info("Executando ciclo de re-matching pos-recovery...");
        List<TradeResult> trades = engine.runRecoveryMatching();

        if (!trades.isEmpty()) {
            for (TradeResult trade : trades) {
                tradeService.executeTrade(trade);
            }
            Log.infof("Re-matching concluido: %d trades executados", trades.size());
        } else {
            Log.info("Re-matching concluido: nenhum trade pendente");
        }

        engine.setReady(true);
        Log.info("Order Book pronto para receber trafego");
    }
}
