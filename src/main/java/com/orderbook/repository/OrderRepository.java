package com.orderbook.repository;

import com.orderbook.entity.Order;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<Order, UUID> {

    public List<Order> findByUserId(UUID userId) {
        return find("user.id", userId).list();
    }

    public List<Order> findOpenOrders() {
        return find("status IN ('NEW', 'PARTIALLY_FILLED') ORDER BY createdAt ASC").list();
    }
}
