package com.orderbook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_history", indexes = {
    @Index(name = "idx_txn_user_id", columnList = "user_id"),
    @Index(name = "idx_txn_trade_id", columnList = "trade_id")
})
public class TransactionHistory extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    public Trade trade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderSide type;

    @Column(precision = 18, scale = 2, nullable = false)
    public BigDecimal price;

    @Column(precision = 18, scale = 8, nullable = false)
    public BigDecimal quantity;

    @Column(name = "total_value", precision = 18, scale = 2, nullable = false)
    public BigDecimal totalValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;
}
