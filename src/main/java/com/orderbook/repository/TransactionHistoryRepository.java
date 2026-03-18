package com.orderbook.repository;

import com.orderbook.entity.TransactionHistory;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TransactionHistoryRepository implements PanacheRepositoryBase<TransactionHistory, UUID> {

    public List<TransactionHistory> findByUserId(UUID userId, int page, int size) {
        return find("user.id = ?1 ORDER BY createdAt DESC", userId).page(page, size).list();
    }

    public long countByUserId(UUID userId) {
        return count("user.id", userId);
    }
}
