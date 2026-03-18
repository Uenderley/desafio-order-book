package com.orderbook.infrastructure;

import com.orderbook.engine.MatchingEngine;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.time.Duration;

@ApplicationScoped
public class OrderBookShutdown {

    @Inject
    MatchingEngine engine;

    void onShutdown(@Observes ShutdownEvent ev) {
        Log.info("Shutdown iniciado — parando de aceitar novas ordens...");

        engine.stopAcceptingOrders();

        engine.drainInFlightOperations(Duration.ofSeconds(10));

        Log.info("Shutdown completo — book sera reconstruido no proximo startup.");
    }
}
