package com.studioelengomes.StudioElenGomes.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtUtil {
    private final String secret;
    private final long expirationSeconds;

    public JwtUtil(@Value("${security.jwt.secret:studio-elen-gomes-change-this-secret}") String secret,
                   @Value("${security.jwt.expiration-seconds:28800}") long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public String generate(String username) {
        long exp = Instant.now().getEpochSecond() + expirationSeconds;
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"sub\":\"" + escape(username) + "\",\"exp\":" + exp + "}");
        String data = header + "." + payload;
        return data + "." + sign(data);
    }

    public String subject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !sign(parts[0] + "." + parts[1]).equals(parts[2])) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            final String subKey = "\"sub\":\"";
            final String expKey = "\"exp\":";
            int subIdx = payload.indexOf(subKey);
            int expIdx = payload.indexOf(expKey);
            if (subIdx < 0 || expIdx < 0) return null;

            // O início do valor é o índice da chave MAIS o tamanho da chave (sem somar a mais)
            int subStart = subIdx + subKey.length();
            int subEnd = payload.indexOf('"', subStart);
            int expStart = expIdx + expKey.length();
            int expEnd = payload.indexOf('}', expStart);
            if (subEnd < 0 || expEnd < 0) return null;

            long exp = Long.parseLong(payload.substring(expStart, expEnd).trim());
            if (Instant.now().getEpochSecond() >= exp) return null;
            return payload.substring(subStart, subEnd);
        } catch (Exception e) { return null; }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private String b64(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
