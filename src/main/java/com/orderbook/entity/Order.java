package com.orderbook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_matching", columnList = "side, status, price, created_at")
})
public class Order extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderSide side;

    @Column(precision = 18, scale = 2, nullable = false)
    public BigDecimal price;

    @Column(precision = 18, scale = 8, nullable = false)
    public BigDecimal quantity;

    @Column(precision = 18, scale = 8, nullable = false)
    public BigDecimal remaining;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
