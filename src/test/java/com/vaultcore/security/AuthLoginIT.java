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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthLoginIT {

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
    void loginSuccess() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
            Map.of("username", "testuser", "password", "password"),
            headers
        );

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            "/auth/login",
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(response.getHeaders().get("Set-Cookie")).isNotEmpty();
    }

    @Test
    void loginFailure() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
            Map.of("username", "testuser", "password", "wrong"),
            headers
        );

        ResponseEntity<String> response = restTemplate.exchange(
            "/auth/login",
            HttpMethod.POST,
            request,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}