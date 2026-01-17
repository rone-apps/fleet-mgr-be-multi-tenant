package com.taxi.security;

import com.taxi.domain.user.model.User;
import com.taxi.domain.user.repository.UserRepository;
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

       // TEMP DEBUG - remove after testing
    // System.out.println(">>> Found user: " + user.getUsername());
    // System.out.println(">>> Stored password hash: " + user.getPassword());
    // System.out.println(">>> Hash length: " + user.getPassword().length()); 

    

    // TEMP DEBUG - test BCrypt matching directly
// org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder testEncoder = 
//     new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
// boolean testMatch = testEncoder.matches("bonny123", user.getPassword());
// System.out.println(">>> BCrypt test match for 'bonny123': " + testMatch);


       
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
