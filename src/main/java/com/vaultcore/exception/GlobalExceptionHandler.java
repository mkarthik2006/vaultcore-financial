package com.vaultcore.exception;

import com.vaultcore.core.fraud.FraudChallengeRequiredException;
import com.vaultcore.core.transfer.IdempotencyConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        // Deliberately generic: never disclose whether a resource exists to a non-owner.
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "access_denied",
            "message", "You do not have permission to perform this action."
        ));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<?> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", "idempotency_conflict",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "error", "validation_failed",
            "details", errors
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "error", "invalid_request",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(FraudChallengeRequiredException.class)
    public ResponseEntity<?> handleFraudChallenge(FraudChallengeRequiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "fraud_challenge_required",
            "message", ex.getMessage(),
            "channel", ex.getChannel(),
            "challengeId", ex.getChallengeId().toString(),
            "expiresAt", ex.getExpiresAt().toString(),
            "verifyUrl", "/api/v1/fraud/challenges/" + ex.getChallengeId() + "/verify",
            "resubmitHeader", "X-Fraud-Challenge-Id"
        ));
    }
}