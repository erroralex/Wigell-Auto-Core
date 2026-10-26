package com.wac.autocore.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityBeansTest {

    private final PasswordEncoder encoder = new SecurityBeans().passwordEncoder();

    @Test
    void encodesWithBcryptPrefixAndVerifies() {
        String hash = encoder.encode("secret");
        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(encoder.matches("secret", hash));
        assertFalse(encoder.matches("wrong", hash));
    }
}
