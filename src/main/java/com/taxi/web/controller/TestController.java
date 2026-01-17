package com.taxi.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello! Backend is working!");
    }

    @GetMapping("/auth-test")
    public ResponseEntity<String> authTest() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return ResponseEntity.ok("Authenticated as: " + auth.getName() + ", Authorities: " + auth.getAuthorities());
        }
        return ResponseEntity.ok("Not authenticated");
    }
}
