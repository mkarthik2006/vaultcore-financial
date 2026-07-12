package com.vaultcore.core.account;

import com.vaultcore.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.UUID;

@Entity
@Table(name = "accounts")
// Accounts are read-mostly and looked up by number on every transfer/balance call: opt them into the
// Redis-backed Hibernate second-level cache (inert in the test profile, which disables L2).
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "accounts")
public class Account {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 32)
    private String accountNumber;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity owner;

    protected Account() {
        // JPA
    }

    public Account(String accountNumber, String currency) {
        this.accountNumber = accountNumber;
        this.currency = currency;
    }

    public Account(String accountNumber, String currency, UserEntity owner) {
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.owner = owner;
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCurrency() {
        return currency;
    }

    public UserEntity getOwner() {
        return owner;
    }
}