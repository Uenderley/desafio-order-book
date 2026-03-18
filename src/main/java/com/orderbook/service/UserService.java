package com.orderbook.service;

import com.orderbook.entity.User;
import com.orderbook.entity.Wallet;
import com.orderbook.exception.UserNotFoundException;
import com.orderbook.repository.UserRepository;
import com.orderbook.repository.WalletRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
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

    public Wallet getWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
