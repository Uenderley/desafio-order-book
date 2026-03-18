package com.orderbook.infrastructure;

import com.orderbook.engine.MatchingEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class OrderBookReadinessCheck implements HealthCheck {

    @Inject
    MatchingEngine engine;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("order-book")
                .status(engine.isReady())
                .withData("ordersLoaded", engine.getOrderCount())
                .build();
    }
}
