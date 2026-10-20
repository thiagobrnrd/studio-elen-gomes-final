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
    private final com.studioelengomes.StudioElenGomes.service.LimiteRequisicoes limite;

    public AuthApiController(JwtUtil jwtUtil,
                             com.studioelengomes.StudioElenGomes.service.LimiteRequisicoes limite,
                             @Value("${app.admin.user}") String adminUser,
                             @Value("${app.admin.password}") String adminPassword,
                             @Value("${security.jwt.secret}") String jwtSecret) {
        this.jwtUtil = jwtUtil; this.limite = limite; this.adminUser = adminUser; this.adminPassword = adminPassword;
        // Em produção (Render), não sobe com senha/chave padrão do código.
        if (System.getenv("RENDER") != null) {
            if ("admin123".equals(adminPassword) || adminPassword.length() < 8)
                throw new IllegalStateException("Defina ADMIN_PASSWORD (mínimo 8 caracteres) nas variáveis de ambiente.");
            if (jwtSecret.startsWith("troque-esta-chave") || jwtSecret.length() < 32)
                throw new IllegalStateException("Defina JWT_SECRET (mínimo 32 caracteres) nas variáveis de ambiente.");
        }
    }

    private static String ip(jakarta.servlet.http.HttpServletRequest r) {
        String f = r.getHeader("X-Forwarded-For");
        return f != null && !f.isBlank() ? f.split(",")[0].trim() : r.getRemoteAddr();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, jakarta.servlet.http.HttpServletRequest http) {
        // Contra tentativa de adivinhar a senha: 8 tentativas a cada 15 min por IP.
        if (!limite.permitir("login:" + ip(http), 8, 15 * 60 * 1000L)) {
            return ResponseEntity.status(429).body(java.util.Map.of("message", "Muitas tentativas. Aguarde 15 minutos e tente de novo."));
        }
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
