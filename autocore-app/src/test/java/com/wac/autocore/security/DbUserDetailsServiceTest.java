package com.wac.autocore.security;

import com.wac.autocore.model.AppUser;
import com.wac.autocore.repository.AppUserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbUserDetailsServiceTest {

    private final AppUserRepo repo = mock(AppUserRepo.class);
    private final DbUserDetailsService service = new DbUserDetailsService(repo);

    private void stub(String role, boolean enabled) {
        when(repo.findByUsername("anna"))
                .thenReturn(Optional.of(new AppUser("anna", "{bcrypt}hash", role, enabled)));
    }

    @Test
    void mapsRoleToRolePrefixedAuthority() {
        stub("WORKSHOP", true);
        UserDetails user = service.loadUserByUsername("anna");
        assertEquals("anna", user.getUsername());
        assertEquals("{bcrypt}hash", user.getPassword());
        assertTrue(user.isEnabled());
        assertEquals(1, user.getAuthorities().size());
        assertEquals("ROLE_WORKSHOP", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void disabledUserIsNotEnabled() {
        stub("ADMIN", false);
        assertFalse(service.loadUserByUsername("anna").isEnabled());
    }

    @Test
    void unknownUserIsRejected() {
        when(repo.findByUsername("nobody")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nobody"));
    }

    @Test
    void invalidRoleIsRejected() {
        stub("SUPERUSER", true);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("anna"));
    }

    @Test
    void nullRoleIsRejected() {
        stub(null, true);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("anna"));
    }
}