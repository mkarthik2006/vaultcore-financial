package com.vaultcore.security;

import com.vaultcore.user.UserRepository;
import com.vaultcore.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthRefreshRotationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        userRepository.save(new UserEntity(
            UUID.randomUUID(),
            "testuser@example.com",
            "testuser",
            passwordEncoder.encode("password"),
            true,
            "USER"
        ));
    }

    @Test
    void refreshRotationInvalidatesOldToken() {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> loginRequest = new HttpEntity<>(
            Map.of("username", "testuser", "password", "password"),
            loginHeaders
        );

        ResponseEntity<Map<String, Object>> loginResponse = restTemplate.exchange(
            "/auth/login",
            HttpMethod.POST,
            loginRequest,
            new ParameterizedTypeReference<>() {}
        );

        List<String> cookies = loginResponse.getHeaders().get("Set-Cookie");
        assertThat(cookies).isNotEmpty();

        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.put(HttpHeaders.COOKIE, cookies);

        ResponseEntity<Map<String, Object>> refreshResponse = restTemplate.exchange(
            "/auth/refresh",
            HttpMethod.POST,
            new HttpEntity<>(refreshHeaders),
            new ParameterizedTypeReference<>() {}
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> newCookies = refreshResponse.getHeaders().get("Set-Cookie");
        assertThat(newCookies).isNotEmpty();

        ResponseEntity<String> replayResponse = restTemplate.exchange(
            "/auth/refresh",
            HttpMethod.POST,
            new HttpEntity<>(refreshHeaders),
            String.class
        );

        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}