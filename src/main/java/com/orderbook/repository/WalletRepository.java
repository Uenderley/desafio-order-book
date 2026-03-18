package com.orderbook.repository;

import com.orderbook.entity.Wallet;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WalletRepository implements PanacheRepositoryBase<Wallet, UUID> {

    public Optional<Wallet> findByUserId(UUID userId) {
        return find("user.id", userId).firstResultOptional();
    }
}
