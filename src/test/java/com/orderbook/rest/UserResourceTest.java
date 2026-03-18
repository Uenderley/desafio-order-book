package com.orderbook.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserResourceTest {

    @Test
    void deveCriarUsuarioComWallet() {
        given()
            .contentType("application/json")
            .body("""
                {"name": "Wakandiano", "email": "user-res-%s@example.com"}
                """.formatted(UUID.randomUUID()))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is("Wakandiano"))
            .body("email", notNullValue());
    }

    @Test
    void deveRetornar400QuandoEmailDuplicado() {
        String email = "dup-res-%s@example.com".formatted(UUID.randomUUID());

        given()
            .contentType("application/json")
            .body("""
                {"name": "User 1", "email": "%s"}
                """.formatted(email))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201);

        given()
            .contentType("application/json")
            .body("""
                {"name": "User 2", "email": "%s"}
                """.formatted(email))
        .when()
            .post("/api/users")
        .then()
            .statusCode(400);
    }

    @Test
    void deveConsultarWalletDoUsuario() {
        String userId = given()
            .contentType("application/json")
            .body("""
                {"name": "Wallet User", "email": "wallet-res-%s@example.com"}
                """.formatted(UUID.randomUUID()))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
        .when()
            .get("/api/users/{id}/wallet", userId)
        .then()
            .statusCode(200)
            .body("balanceBrl", notNullValue())
            .body("balanceVibranium", notNullValue())
            .body("reservedBrl", notNullValue())
            .body("reservedVibranium", notNullValue());
    }

    @Test
    void deveRetornar404QuandoUsuarioNaoExiste() {
        given()
        .when()
            .get("/api/users/{id}/wallet", UUID.randomUUID())
        .then()
            .statusCode(404);
    }

    @Test
    void deveConsultarTransacoesDoUsuario() {
        String userId = given()
            .contentType("application/json")
            .body("""
                {"name": "Txn User", "email": "txn-res-%s@example.com"}
                """.formatted(UUID.randomUUID()))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
        .when()
            .get("/api/users/{id}/transactions", userId)
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", is(0))
            .body("size", is(20))
            .body("totalElements", notNullValue());
    }
}
