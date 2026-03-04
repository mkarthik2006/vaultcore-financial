package com.vaultcore.exception;

import com.vaultcore.core.fraud.FraudChallengeRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
            "channel", ex.getChannel()
        ));
    }
}