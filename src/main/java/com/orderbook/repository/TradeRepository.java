package com.orderbook.repository;

import com.orderbook.entity.Trade;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TradeRepository implements PanacheRepositoryBase<Trade, UUID> {

    public List<Trade> findByOrderId(UUID orderId) {
        return find("buyOrder.id = ?1 OR sellOrder.id = ?1", orderId).list();
    }

    public List<Trade> listRecent(int page, int size) {
        return find("ORDER BY executedAt DESC").page(page, size).list();
    }
}
