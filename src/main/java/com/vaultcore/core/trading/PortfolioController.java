package com.vaultcore.core.trading;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public PortfolioDTO getPortfolio(Principal principal) {
        return portfolioService.getPortfolioForUser(resolveUsername(principal));
    }

    @PostMapping("/holdings")
    public ResponseEntity<PortfolioDTO> addHolding(@Valid @RequestBody AddHoldingRequest request,
                                                   Principal principal) {
        PortfolioDTO response = portfolioService.addHolding(resolveUsername(principal), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/valuation")
    public PortfolioDTO getValuation(Principal principal) {
        return portfolioService.getValuation(resolveUsername(principal));
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