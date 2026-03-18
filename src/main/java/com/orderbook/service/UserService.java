package com.orderbook.service;

import com.orderbook.dto.UserWithWalletResponse;
import com.orderbook.entity.User;
import com.orderbook.entity.Wallet;
import com.orderbook.exception.UserNotFoundException;
import com.orderbook.repository.UserRepository;
import com.orderbook.repository.WalletRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    WalletRepository walletRepository;

    @Transactional
    public User createUser(String name, String email, BigDecimal initialBrl, BigDecimal initialVibranium) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email ja cadastrado: " + email);
        }

        User user = new User();
        user.name = name;
        user.email = email;
        userRepository.persist(user);

        Wallet wallet = new Wallet();
        wallet.user = user;
        wallet.balanceBrl = initialBrl != null ? initialBrl : BigDecimal.ZERO;
        wallet.balanceVibranium = initialVibranium != null ? initialVibranium : BigDecimal.ZERO;
        wallet.reservedBrl = BigDecimal.ZERO;
        wallet.reservedVibranium = BigDecimal.ZERO;
        walletRepository.persist(wallet);

        return user;
    }

    public List<UserWithWalletResponse> listUsersWithWallet(int page, int size) {
        List<User> users = userRepository.find("ORDER BY createdAt DESC").page(page, size).list();
        return users.stream().map(user -> {
            Wallet wallet = walletRepository.findByUserId(user.id).orElseThrow();
            return UserWithWalletResponse.from(user, wallet);
        }).toList();
    }

    public long countUsers() {
        return userRepository.count();
    }

    @Transactional
    public Wallet deposit(UUID userId, BigDecimal amountBrl, BigDecimal amountVibranium) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (amountBrl != null && amountBrl.signum() > 0) {
            wallet.balanceBrl = wallet.balanceBrl.add(amountBrl);
        }
        if (amountVibranium != null && amountVibranium.signum() > 0) {
            wallet.balanceVibranium = wallet.balanceVibranium.add(amountVibranium);
        }

        return wallet;
    }

    public Wallet getWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
