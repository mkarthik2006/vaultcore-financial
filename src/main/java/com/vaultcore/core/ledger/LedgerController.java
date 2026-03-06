package com.vaultcore.core.ledger;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerQueryService ledgerQueryService;

    public LedgerController(LedgerQueryService ledgerQueryService) {
        this.ledgerQueryService = ledgerQueryService;
    }

    @GetMapping("/balance")
    public BigDecimal getBalance(@RequestParam String accountNumber,
                                 @RequestParam String currency) {
        return ledgerQueryService.getBalance(accountNumber, currency);
    }
}