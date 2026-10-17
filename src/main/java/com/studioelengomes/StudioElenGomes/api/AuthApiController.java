package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.api.dto.*;
import com.studioelengomes.StudioElenGomes.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final JwtUtil jwtUtil;
    private final String adminUser;
    private final String adminPassword;

    public AuthApiController(JwtUtil jwtUtil,
                             @Value("${app.admin.user}") String adminUser,
                             @Value("${app.admin.password}") String adminPassword) {
        this.jwtUtil = jwtUtil; this.adminUser = adminUser; this.adminPassword = adminPassword;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String username = req.username() == null ? "" : req.username().trim();
        String password = req.password() == null ? "" : req.password();

        boolean userOk = java.security.MessageDigest.isEqual(
                adminUser.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                username.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        boolean passOk = java.security.MessageDigest.isEqual(
                adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!userOk || !passOk) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Usuário ou senha inválidos"));
        }

        return ResponseEntity.ok(new LoginResponse(jwtUtil.generate(adminUser), adminUser));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(java.util.Map.of("username", authentication.getName()));
    }
}
