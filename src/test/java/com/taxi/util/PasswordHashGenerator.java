package com.taxi.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for creating admin users
 * Run this to get the correct hash for your passwords
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Generate hashes for common passwords
        String[] passwords = {
            "password",
            "Admin@2026",
            "admin",
            "YellowCabs@2026"
        };

        System.out.println("=== BCrypt Password Hash Generator ===\n");

        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("Password: " + password);
            System.out.println("Hash: " + hash);
            System.out.println("Verify: " + encoder.matches(password, hash));
            System.out.println("---");
        }

        // Test the existing hash from database
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        System.out.println("\n=== Testing Existing Hash ===");
        System.out.println("Hash: " + existingHash);
        System.out.println("Matches 'Admin@2026': " + encoder.matches("Admin@2026", existingHash));
        System.out.println("Matches 'password': " + encoder.matches("password", existingHash));
        System.out.println("Matches 'admin': " + encoder.matches("admin", existingHash));
    }
}
