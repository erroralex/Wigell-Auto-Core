package com.wac.autocore.service;

import com.wac.autocore.model.AppUser;
import com.wac.autocore.repository.AppUserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <b>AppUserDetailsServiceTest</b>
 * <p>Responsibility: Proves that a user's role becomes a ROLE_ authority, and that an unknown or missing role
 * rejects the login instead of granting rights.</p>
 */
class AppUserDetailsServiceTest {

    private final AppUserRepo repo = mock(AppUserRepo.class);
    private final AppUserDetailsService service = new AppUserDetailsService(repo);

    private void stubUser(String role, boolean enabled) {
        when(repo.findByUsername("anna"))
                .thenReturn(Optional.of(new AppUser("anna", "{bcrypt}hash", role, enabled)));
    }

    @Test
    @DisplayName("a known role becomes a ROLE_ prefixed authority")
    void knownRoleGetsRolePrefix() {
        stubUser("WORKSHOP", true);

        UserDetails user = service.loadUserByUsername("anna");

        assertEquals("anna", user.getUsername());
        assertEquals("{bcrypt}hash", user.getPassword());
        assertTrue(user.isEnabled());
        assertEquals(1, user.getAuthorities().size());
        assertEquals("ROLE_WORKSHOP", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("READ_ONLY keeps its underscore in the authority")
    void readOnlyRoleKeepsUnderscore() {
        stubUser("READ_ONLY", true);

        UserDetails user = service.loadUserByUsername("anna");

        assertEquals("ROLE_READ_ONLY", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("a disabled user is not enabled")
    void disabledUserIsNotEnabled() {
        stubUser("ADMIN", false);

        assertFalse(service.loadUserByUsername("anna").isEnabled());
    }

    @Test
    @DisplayName("an unknown role rejects the login")
    void unknownRoleIsRejected() {
        stubUser("SUPERUSER", true);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("anna"));
    }

    @Test
    @DisplayName("a missing role rejects the login")
    void nullRoleIsRejected() {
        stubUser(null, true);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("anna"));
    }

    @Test
    @DisplayName("a lower case role is rejected, not silently accepted")
    void lowerCaseRoleIsRejected() {
        stubUser("admin", true);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("anna"));
    }

    @Test
    @DisplayName("an unknown user is rejected")
    void unknownUserIsRejected() {
        when(repo.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nobody"));
    }
}
