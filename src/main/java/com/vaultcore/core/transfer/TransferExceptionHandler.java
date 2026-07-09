package com.vaultcore.core.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Transfer-domain exceptions. Cross-cutting concerns (fraud challenge, idempotency conflict,
 * access denied, validation, generic IllegalArgument) are owned exclusively by
 * {@link com.vaultcore.exception.GlobalExceptionHandler} to avoid duplicate/competing advice — this
 * class only maps exceptions unique to the transfer domain.
 */
@RestControllerAdvice
public class TransferExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(404, ex.getMessage()));
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ApiError> handleCurrencyMismatch(CurrencyMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(400, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(409, ex.getMessage()));
    }

    public record ApiError(int status, String message, Instant timestamp) {
        static ApiError of(int status, String message) {
            return new ApiError(status, message, Instant.now());
        }
    }
}