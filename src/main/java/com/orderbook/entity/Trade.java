package com.orderbook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"buy_order_id", "sell_order_id"})
}, indexes = {
    @Index(name = "idx_trades_buy_order", columnList = "buy_order_id"),
    @Index(name = "idx_trades_sell_order", columnList = "sell_order_id")
})
public class Trade extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "buy_order_id", nullable = false)
    public Order buyOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sell_order_id", nullable = false)
    public Order sellOrder;

    @Column(precision = 18, scale = 2, nullable = false)
    public BigDecimal price;

    @Column(precision = 18, scale = 8, nullable = false)
    public BigDecimal quantity;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false, updatable = false)
    public Instant executedAt;
}
