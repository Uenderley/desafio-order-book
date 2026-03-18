package com.orderbook.repository;

import com.orderbook.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserRepositoryTest {

    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    void deveBuscarUsuarioPorEmail() {
        User user = new User();
        user.name = "Wakandiano";
        user.email = "find-by-email@example.com";
        userRepository.persist(user);

        Optional<User> found = userRepository.findByEmail("find-by-email@example.com");

        assertTrue(found.isPresent());
        assertEquals("Wakandiano", found.get().name);
    }

    @Test
    @Transactional
    void deveRetornarVazioQuandoEmailNaoExiste() {
        Optional<User> found = userRepository.findByEmail("nao-existe@example.com");

        assertTrue(found.isEmpty());
    }
}
