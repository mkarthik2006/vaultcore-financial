package com.vaultcore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Always-on security support (active in ALL profiles, including "test").
 *
 * <p>Method-level security is enabled here rather than on {@code SecurityConfig} (which is
 * {@code @Profile("!test")}) so that {@code @PreAuthorize}/object-level authorization rules are
 * actually enforced — and therefore testable — under the {@code test} profile as well. This closes
 * the audit finding that authorization was disabled in the only environment that runs the test
 * suite.</p>
 *
 * <p>The {@link PasswordEncoder} lives here too so it is available to provisioning code regardless
 * of profile, replacing the previous plaintext credential storage.</p>
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
