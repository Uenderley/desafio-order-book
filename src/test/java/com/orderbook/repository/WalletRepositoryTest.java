package com.orderbook.repository;

import com.orderbook.entity.User;
import com.orderbook.entity.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WalletRepositoryTest {

    @Inject
    WalletRepository walletRepository;

    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    void deveBuscarWalletPorUserId() {
        User user = createUser("wallet-repo@example.com");
        createWallet(user, "5000.00", "20.00000000");

        Optional<Wallet> found = walletRepository.findByUserId(user.id);

        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("5000.00").compareTo(found.get().balanceBrl));
        assertEquals(0, new BigDecimal("20.00000000").compareTo(found.get().balanceVibranium));
    }

    @Test
    @Transactional
    void deveRetornarVazioQuandoUserIdNaoTemWallet() {
        User user = createUser("sem-wallet@example.com");

        Optional<Wallet> found = walletRepository.findByUserId(user.id);

        assertTrue(found.isEmpty());
    }

    private User createUser(String email) {
        User user = new User();
        user.name = "Test User";
        user.email = email;
        userRepository.persist(user);
        return user;
    }

    private Wallet createWallet(User user, String brl, String vibranium) {
        Wallet wallet = new Wallet();
        wallet.user = user;
        wallet.balanceBrl = new BigDecimal(brl);
        wallet.balanceVibranium = new BigDecimal(vibranium);
        wallet.reservedBrl = BigDecimal.ZERO;
        wallet.reservedVibranium = BigDecimal.ZERO;
        walletRepository.persist(wallet);
        return wallet;
    }
}
