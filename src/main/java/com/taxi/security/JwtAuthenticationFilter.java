package com.taxi.security;

import com.taxi.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the Authorization header
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // Remove "Bearer " prefix

            if (jwtUtils.validateJwtToken(token)) {
                try {
                    // Extract username from the token
                    String username = jwtUtils.extractUsername(token);

                    // Load user details using the username
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // Log authorities for debugging
                    logger.info("Authenticating user: {}, Authorities: {}", username, userDetails.getAuthorities());

                    // Create an authentication token
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // Set the authentication context
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // Store userId in request attributes for further use if needed
                    Long userId = jwtUtils.getUserIdFromJwtToken(token);
                    request.setAttribute("userId", userId);

                } catch (Exception e) {
                    logger.error("Failed to set user authentication: {}", e.getMessage());
                }
            }
        }

        // Continue with the next filter in the chain
        filterChain.doFilter(request, response);
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only skip filter for login and signup endpoints
        // /auth/me and other endpoints NEED JWT validation
        return path.equals("/auth/signup") || path.equals("/auth/login") ||
               path.equals("/api/auth/signup") || path.equals("/api/auth/login");
    }

}
