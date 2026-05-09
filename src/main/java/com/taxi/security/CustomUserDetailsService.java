package com.taxi.security;

import com.taxi.domain.user.model.User;
import com.taxi.domain.user.repository.UserRepository;
import com.taxi.infrastructure.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetailsService implementation for Spring Security
 * Loads user details from the User entity
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String currentTenant = TenantContext.getCurrentTenant();

        System.out.println(">>> ==========================================");
        System.out.println(">>> CURRENT TENANT/SCHEMA: " + currentTenant);
        System.out.println(">>> ATTEMPTING TO LOAD USER: " + username);
        System.out.println(">>> ==========================================");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println(">>> USER NOT FOUND IN SCHEMA: " + currentTenant);
                    System.out.println(">>> USERNAME SEARCHED: " + username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        System.out.println(">>> FOUND USER: " + user.getUsername());
        System.out.println(">>> EMAIL: " + user.getEmail());
        System.out.println(">>> ROLE: " + user.getRole());
        System.out.println(">>> IS_ACTIVE: " + user.isActive());
        System.out.println(">>> STORED PASSWORD HASH: " + user.getPassword());
        System.out.println(">>> HASH LENGTH: " + user.getPassword().length());

        // Test BCrypt matching with the password you're trying
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder testEncoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        boolean testMatchAdmin = testEncoder.matches("Admin@2026", user.getPassword());
        boolean testMatchPassword = testEncoder.matches("password", user.getPassword());
        System.out.println(">>> BCRYPT TEST MATCH FOR 'Admin@2026': " + testMatchAdmin);
        System.out.println(">>> BCRYPT TEST MATCH FOR 'password': " + testMatchPassword);
        System.out.println(">>> ==========================================");


       
                if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is not active: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                true,  // accountNonLocked
                getAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        String role = "ROLE_" + user.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    /**
     * Load user entity by username (for use in controllers)
     */
    public User loadUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
