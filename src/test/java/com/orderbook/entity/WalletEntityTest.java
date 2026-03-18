package com.orderbook.entity;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WalletEntityTest {

    @Inject
    EntityManager em;

    @Test
    @Transactional
    void devePersistirWalletComSaldosIniciais() {
        User user = new User();
        user.name = "Trader";
        user.email = "trader-wallet@example.com";
        em.persist(user);

        Wallet wallet = new Wallet();
        wallet.user = user;
        wallet.balanceBrl = new BigDecimal("10000.00");
        wallet.balanceVibranium = new BigDecimal("50.00000000");
        wallet.reservedBrl = BigDecimal.ZERO;
        wallet.reservedVibranium = BigDecimal.ZERO;

        em.persist(wallet);
        em.flush();

        assertNotNull(wallet.id);
        assertEquals(0, new BigDecimal("10000.00").compareTo(wallet.balanceBrl));
        assertEquals(0, new BigDecimal("50.00000000").compareTo(wallet.balanceVibranium));
    }

    @Test
    @Transactional
    void deveAplicarOptimisticLocking() {
        User user = new User();
        user.name = "Optimistic";
        user.email = "optimistic@example.com";
        em.persist(user);

        Wallet wallet = new Wallet();
        wallet.user = user;
        wallet.balanceBrl = new BigDecimal("1000.00");
        wallet.balanceVibranium = BigDecimal.ZERO;
        wallet.reservedBrl = BigDecimal.ZERO;
        wallet.reservedVibranium = BigDecimal.ZERO;
        em.persist(wallet);
        em.flush();

        // version deve ser inicializada pelo JPA
        assertNotNull(wallet.version);
    }

    @Test
    @Transactional
    void userIdDeveSerUnicoNaWallet() {
        User user = new User();
        user.name = "Unique Wallet";
        user.email = "unique-wallet@example.com";
        em.persist(user);

        Wallet wallet1 = new Wallet();
        wallet1.user = user;
        wallet1.balanceBrl = BigDecimal.ZERO;
        wallet1.balanceVibranium = BigDecimal.ZERO;
        wallet1.reservedBrl = BigDecimal.ZERO;
        wallet1.reservedVibranium = BigDecimal.ZERO;
        em.persist(wallet1);
        em.flush();

        Wallet wallet2 = new Wallet();
        wallet2.user = user;
        wallet2.balanceBrl = BigDecimal.ZERO;
        wallet2.balanceVibranium = BigDecimal.ZERO;
        wallet2.reservedBrl = BigDecimal.ZERO;
        wallet2.reservedVibranium = BigDecimal.ZERO;

        assertThrows(Exception.class, () -> {
            em.persist(wallet2);
            em.flush();
        });
    }
}
