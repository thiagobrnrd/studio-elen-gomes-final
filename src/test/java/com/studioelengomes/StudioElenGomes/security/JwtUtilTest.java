package com.studioelengomes.StudioElenGomes.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void tokenRecemGeradoDevolveOUsuarioCompleto() {
        JwtUtil jwt = new JwtUtil("segredo-de-teste", 3600);
        String token = jwt.generate("elen");
        assertEquals("elen", jwt.subject(token));
    }

    @Test
    void tokenExpiradoERejeitado() {
        JwtUtil jwt = new JwtUtil("segredo-de-teste", -10);
        assertNull(jwt.subject(jwt.generate("elen")));
    }

    @Test
    void tokenAdulteradoOuComOutroSegredoERejeitado() {
        String token = new JwtUtil("segredo-a", 3600).generate("elen");
        assertNull(new JwtUtil("segredo-b", 3600).subject(token));
        assertNull(new JwtUtil("segredo-a", 3600).subject(token + "x"));
    }
}
