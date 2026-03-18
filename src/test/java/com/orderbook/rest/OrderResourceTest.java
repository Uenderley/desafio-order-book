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
class OrderResourceTest {

    @Inject
    MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        matchingEngine.clear();
    }

    @Test
    void deveCriarOrdemDeCompra() {
        String userId = createUser();

        given()
            .contentType("application/json")
            .header("X-User-Id", userId)
            .body("""
                {"side": "BUY", "price": 150.00, "quantity": 10.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("side", is("BUY"))
            .body("status", is("NEW"))
            .body("price", is(150.00f))
            .body("quantity", is(10.0f));
    }

    @Test
    void deveCriarOrdemDeVenda() {
        String userId = createUser();

        given()
            .contentType("application/json")
            .header("X-User-Id", userId)
            .body("""
                {"side": "SELL", "price": 155.00, "quantity": 5.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("side", is("SELL"))
            .body("status", is("NEW"));
    }

    @Test
    void deveRetornar400QuandoSaldoInsuficiente() {
        String userId = createUser();

        given()
            .contentType("application/json")
            .header("X-User-Id", userId)
            .body("""
                {"side": "BUY", "price": 150.00, "quantity": 1000000.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400)
            .body("error", containsString("insuficiente"));
    }

    @Test
    void deveConsultarOrdemPorId() {
        String userId = createUser();

        String orderId = given()
            .contentType("application/json")
            .header("X-User-Id", userId)
            .body("""
                {"side": "BUY", "price": 150.00, "quantity": 5.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
        .when()
            .get("/api/orders/{id}", orderId)
        .then()
            .statusCode(200)
            .body("id", is(orderId))
            .body("side", is("BUY"));
    }

    @Test
    void deveCancelarOrdem() {
        String userId = createUser();

        String orderId = given()
            .contentType("application/json")
            .header("X-User-Id", userId)
            .body("""
                {"side": "BUY", "price": 150.00, "quantity": 5.0}
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .header("X-User-Id", userId)
        .when()
            .delete("/api/orders/{id}", orderId)
        .then()
            .statusCode(200)
            .body("status", is("CANCELLED"));
    }

    @Test
    void deveRetornar404QuandoOrdemNaoExiste() {
        given()
        .when()
            .get("/api/orders/{id}", UUID.randomUUID())
        .then()
            .statusCode(404);
    }

    private String createUser() {
        return given()
            .contentType("application/json")
            .body("""
                {"name": "Order User", "email": "order-res-%s@example.com",
                 "initialBalanceBrl": 100000.00, "initialBalanceVibranium": 1000.0}
                """.formatted(UUID.randomUUID()))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .extract().path("id");
    }
}
