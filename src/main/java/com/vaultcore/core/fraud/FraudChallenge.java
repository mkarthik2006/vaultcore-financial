package com.vaultcore.core.fraud;

import com.vaultcore.core.transfer.TransferRequestDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A single 2FA fraud challenge tied to a specific (fromAccount, amount, currency) transfer intent.
 */
@Entity
@Table(name = "fraud_challenges")
public class FraudChallenge {

    public enum Status { PENDING, VERIFIED, CONSUMED, EXPIRED }

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "from_account", nullable = false, updatable = false, length = 32)
    private String fromAccount;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "code_hash", nullable = false, updatable = false, length = 128)
    private String codeHash;

    @Column(name = "channel", nullable = false, updatable = false, length = 40)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected FraudChallenge() {
        // JPA
    }

    public FraudChallenge(String fromAccount, BigDecimal amount, String currency,
                          String codeHash, String channel, Instant expiresAt) {
        this.fromAccount = fromAccount;
        this.amount = amount;
        this.currency = currency;
        this.codeHash = codeHash;
        this.channel = channel;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    /** True when this challenge corresponds to the given transfer intent. */
    public boolean matches(TransferRequestDTO request) {
        return fromAccount.equals(request.fromAccount())
            && currency.equalsIgnoreCase(request.currency())
            && amount.compareTo(request.amount()) == 0;
    }

    public void markVerified() {
        this.status = Status.VERIFIED;
        this.verifiedAt = Instant.now();
    }

    public void markConsumed() {
        this.status = Status.CONSUMED;
        this.consumedAt = Instant.now();
    }

    public void markExpired() {
        this.status = Status.EXPIRED;
    }

    public UUID getId() { return id; }
    public String getFromAccount() { return fromAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCodeHash() { return codeHash; }
    public String getChannel() { return channel; }
    public Status getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
}
