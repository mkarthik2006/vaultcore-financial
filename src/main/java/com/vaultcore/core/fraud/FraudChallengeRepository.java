package com.vaultcore.core.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FraudChallengeRepository extends JpaRepository<FraudChallenge, UUID> {
}
