package com.orderbook.infrastructure;

import com.orderbook.engine.MatchingEngine;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class HealthCheckTest {

    @Inject
    MatchingEngine matchingEngine;

    // === Liveness ===

    @Test
    void livenessDeveRetornarUp() {
        given()
        .when()
            .get("/q/health/live")
        .then()
            .statusCode(200)
            .body("status", is("UP"))
            .body("checks.name", hasItem("matching-engine"));
    }

    @Test
    void livenessDeveConterDadosDoEngine() {
        given()
        .when()
            .get("/q/health/live")
        .then()
            .statusCode(200)
            .body("checks.find { it.name == 'matching-engine' }.data.responding", is(true));
    }

    // === Readiness ===

    @Test
    void readinessDeveRetornarUpQuandoBookReconstruido() {
        // A recovery ja rodou no startup, entao ready=true
        matchingEngine.setReady(true);

        given()
        .when()
            .get("/q/health/ready")
        .then()
            .statusCode(200)
            .body("status", is("UP"))
            .body("checks.name", hasItem("order-book"));
    }

    @Test
    void readinessDeveConterContagemDeOrdens() {
        matchingEngine.setReady(true);

        given()
        .when()
            .get("/q/health/ready")
        .then()
            .statusCode(200)
            .body("checks.find { it.name == 'order-book' }.data.ordersLoaded", notNullValue());
    }

    @Test
    void readinessDeveRetornarDownQuandoBookNaoReconstruido() {
        matchingEngine.clear(); // reseta ready=false

        given()
        .when()
            .get("/q/health/ready")
        .then()
            .statusCode(503)
            .body("status", is("DOWN"));

        // Restaura para nao afetar outros testes
        matchingEngine.setReady(true);
    }

    // === Health (combined) ===

    @Test
    void healthDeveRetornarUpQuandoTudoOk() {
        matchingEngine.setReady(true);

        given()
        .when()
            .get("/q/health")
        .then()
            .statusCode(200)
            .body("status", is("UP"));
    }
}
