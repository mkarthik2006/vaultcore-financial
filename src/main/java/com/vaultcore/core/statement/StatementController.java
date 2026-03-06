package com.vaultcore.core.statement;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/statements")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> getMonthly(@RequestParam String accountNumber,
                                             @RequestParam String month,
                                             Principal principal) {
        String username = resolveUsername(principal);
        YearMonth ym = YearMonth.parse(month);

        byte[] pdf = statementService.generateMonthlyStatement(accountNumber, ym, username);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=statement-" + accountNumber + "-" + month + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    private String resolveUsername(Principal principal) {
        if (principal instanceof JwtAuthenticationToken token) {
            String preferred = token.getToken().getClaimAsString("preferred_username");
            if (preferred != null && !preferred.isBlank()) {
                return preferred;
            }
        }
        return principal.getName();
    }
}