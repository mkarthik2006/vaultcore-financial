package com.vaultcore.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestProtectedController {

    @GetMapping("/protected")
    String protectedEndpoint() {
        return "ok";
    }
}