package com.orderbook.entity;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserEntityTest {

    @Inject
    EntityManager em;

    @Test
    @Transactional
    void devePersistirERecuperarUsuario() {
        User user = new User();
        user.name = "Wakandiano";
        user.email = "wakanda@example.com";

        em.persist(user);
        em.flush();

        assertNotNull(user.id);
        assertNotNull(user.createdAt);

        User found = em.find(User.class, user.id);
        assertEquals("Wakandiano", found.name);
        assertEquals("wakanda@example.com", found.email);
    }

    @Test
    @Transactional
    void emailDeveSerUnico() {
        User user1 = new User();
        user1.name = "User 1";
        user1.email = "duplicado@example.com";
        em.persist(user1);
        em.flush();

        User user2 = new User();
        user2.name = "User 2";
        user2.email = "duplicado@example.com";

        assertThrows(Exception.class, () -> {
            em.persist(user2);
            em.flush();
        });
    }
}
