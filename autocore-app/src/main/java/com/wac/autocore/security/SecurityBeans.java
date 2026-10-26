package com.wac.autocore.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;


// Spring requires a PasswordEncoder bean to be present in the application context.
// Encodes and verifies passwords in the authentication process.
// Allows multiple encoding formats, giving flexibility in password storage and verification.
@Configuration
public class SecurityBeans {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
