package com.orderbook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets", uniqueConstraints = {
    @UniqueConstraint(columnNames = "user_id")
})
public class Wallet extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "balance_brl", precision = 18, scale = 2, nullable = false)
    public BigDecimal balanceBrl;

    @Column(name = "balance_vibranium", precision = 18, scale = 8, nullable = false)
    public BigDecimal balanceVibranium;

    @Column(name = "reserved_brl", precision = 18, scale = 2, nullable = false)
    public BigDecimal reservedBrl;

    @Column(name = "reserved_vibranium", precision = 18, scale = 8, nullable = false)
    public BigDecimal reservedVibranium;

    @Version
    public Long version;
}
