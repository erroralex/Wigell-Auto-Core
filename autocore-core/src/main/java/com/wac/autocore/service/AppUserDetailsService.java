package com.wac.autocore.service;

import com.wac.autocore.model.AppUser;
import com.wac.autocore.repository.AppUserRepo;
import com.wac.autocore.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AppUserDetailsService.class);

    private final AppUserRepo appUserRepo;

    public AppUserDetailsService(AppUserRepo appUserRepo) {
        this.appUserRepo = appUserRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // An unknown role rejects the login, so a bad database value can never grant extra rights.
        Role role;
        try {
            role = Role.valueOf(appUser.getRole());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("app_user id {} has an invalid role", appUser.getId());
            throw new UsernameNotFoundException("Invalid role");
        }

        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .disabled(!appUser.isEnabled())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role.name())))
                .build();
    }
}