package com.wac.autocore.config;

import com.wac.autocore.service.AppUserDetailsService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder, ApplicationEventPublisher applicationEventPublisher) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher = new DefaultAuthenticationEventPublisher();
        defaultAuthenticationEventPublisher.setApplicationEventPublisher(applicationEventPublisher);

        ProviderManager providerManager = new ProviderManager(provider);
        providerManager.setAuthenticationEventPublisher(defaultAuthenticationEventPublisher);

        return providerManager;
    }
}