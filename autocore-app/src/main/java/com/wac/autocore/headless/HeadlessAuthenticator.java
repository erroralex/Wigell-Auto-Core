package com.wac.autocore.headless;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Logs the headless runner in with the configured account, through the same AuthenticationManager as the login dialog.
@Component
@EnableConfigurationProperties(HeadlessProperties.class)
public class HeadlessAuthenticator {

    private final AuthenticationManager authenticationManager;
    private final HeadlessProperties properties;

    public HeadlessAuthenticator(AuthenticationManager authenticationManager, HeadlessProperties properties) {
        this.authenticationManager = authenticationManager;
        this.properties = properties;
    }

    // Throws AuthenticationCredentialsNotFoundException if the credentials are missing, and
    // BadCredentialsException (or another AuthenticationException) if they are wrong.
    public void authenticate() {
        if (!properties.hasCredentials()) {
            throw new AuthenticationCredentialsNotFoundException("Headless credentials are missing");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(properties.getUsername(), properties.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void clear() {
        SecurityContextHolder.clearContext();
    }

}
