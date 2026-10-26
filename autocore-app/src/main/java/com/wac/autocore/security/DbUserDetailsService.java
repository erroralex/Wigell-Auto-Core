package com.wac.autocore.security;

import com.wac.autocore.model.AppUser;
import com.wac.autocore.repository.AppUserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Loads users from the app_user table for Spring Security.
// The role string is mapped to the Role enum and exposed as ROLE_<role>.
// A user with an unknown role is rejected, so a bad database value does not grant extra rights.
@Service
public class DbUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(DbUserDetailsService.class);
    private final AppUserRepo repo;

    public DbUserDetailsService(AppUserRepo repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        Role role;
        try {
            role = Role.valueOf(u.getRole());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("User '{}' has invalid role '{}'", username, u.getRole());
            throw new UsernameNotFoundException("Invalid role");
        }
        return User.withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .disabled(!u.isEnabled())
                .authorities("ROLE_" + role.name())
                .build();
    }
}
