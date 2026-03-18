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
class OrderBookResourceTest {

    @Inject
    MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        matchingEngine.clear();
    }

    @Test
    void deveRetornarOrderBookVazio() {
        given()
        .when()
            .get("/api/orderbook")
        .then()
            .statusCode(200)
            .body("bids", hasSize(0))
            .body("asks", hasSize(0));
    }

    @Test
    void deveRetornarOrderBookComOrdens() {
        String userId = createUser();

        // Coloca uma ordem de compra
        given()
            .contentType("application/json")
            .header("X-User-Id", userId)
            .body("""
                {"side": "BUY", "price": 150.00, "quantity": 10.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);

        // Coloca uma ordem de venda
        given()
            .contentType("application/json")
            .header("X-User-Id", userId)
            .body("""
                {"side": "SELL", "price": 160.00, "quantity": 5.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);

        given()
        .when()
            .get("/api/orderbook")
        .then()
            .statusCode(200)
            .body("bids", hasSize(1))
            .body("asks", hasSize(1));
    }

    private String createUser() {
        return given()
            .contentType("application/json")
            .body("""
                {"name": "Book User", "email": "book-res-%s@example.com",
                 "initialBalanceBrl": 100000.00, "initialBalanceVibranium": 1000.0}
                """.formatted(UUID.randomUUID()))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .extract().path("id");
    }
}
