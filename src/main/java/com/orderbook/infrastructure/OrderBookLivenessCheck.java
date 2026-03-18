package com.orderbook.infrastructure;

import com.orderbook.engine.MatchingEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class OrderBookLivenessCheck implements HealthCheck {

    @Inject
    MatchingEngine engine;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("matching-engine")
                .status(engine.isResponding())
                .withData("responding", engine.isResponding())
                .withData("accepting", engine.isAccepting())
                .build();
    }
}
