package com.vaultcore.core.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionReferenceRepository extends JpaRepository<TransactionReference, UUID> {
}