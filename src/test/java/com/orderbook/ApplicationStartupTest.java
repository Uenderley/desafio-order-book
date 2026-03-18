package com.orderbook;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class ApplicationStartupTest {

    @Test
    void applicationStarts() {
        // Valida que a aplicacao Quarkus inicializa sem erros
        // e o health check de liveness responde UP
        given()
            .when().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
    }

    @Test
    void readinessCheckResponds() {
        given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
    }
}
