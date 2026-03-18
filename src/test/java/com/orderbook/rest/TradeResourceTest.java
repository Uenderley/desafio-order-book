package com.orderbook.rest;

import com.orderbook.engine.MatchingEngine;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TradeResourceTest {

    @Inject
    MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        matchingEngine.clear();
    }

    @Test
    void deveListarTradesPaginado() {
        given()
        .when()
            .get("/api/trades")
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", is(0))
            .body("size", is(20))
            .body("totalElements", notNullValue());
    }

    @Test
    void deveListarTradesAposMatch() {
        String buyerId = createUser("trade-res-buyer");
        String sellerId = createUser("trade-res-seller");

        // Seller coloca ordem
        given()
            .contentType("application/json")
            .header("X-User-Id", sellerId)
            .body("""
                {"side": "SELL", "price": 150.00, "quantity": 5.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);

        // Buyer casa com seller
        given()
            .contentType("application/json")
            .header("X-User-Id", buyerId)
            .body("""
                {"side": "BUY", "price": 150.00, "quantity": 5.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);

        given()
        .when()
            .get("/api/trades")
        .then()
            .statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(1)));
    }

    private String createUser(String prefix) {
        return given()
            .contentType("application/json")
            .body("""
                {"name": "%s", "email": "%s-%s@example.com",
                 "initialBalanceBrl": 100000.00, "initialBalanceVibranium": 1000.0}
                """.formatted(prefix, prefix, UUID.randomUUID()))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .extract().path("id");
    }
}
