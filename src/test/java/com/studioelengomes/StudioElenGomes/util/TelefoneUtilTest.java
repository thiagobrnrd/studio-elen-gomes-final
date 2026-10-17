package com.studioelengomes.StudioElenGomes.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelefoneUtilTest {

    @Test
    void celularComMascara() {
        assertEquals(Optional.of("5511999101739"), TelefoneUtil.paraWhatsApp("(11) 99910-1739"));
    }

    @Test
    void celularSoComDigitos() {
        assertEquals(Optional.of("5511999101739"), TelefoneUtil.paraWhatsApp("11999101739"));
    }

    @Test
    void jaComCodigoDoPais() {
        assertEquals(Optional.of("5511999101739"), TelefoneUtil.paraWhatsApp("+55 11 99910-1739"));
        assertEquals(Optional.of("5511999101739"), TelefoneUtil.paraWhatsApp("5511999101739"));
    }

    @Test
    void zeroNaFrenteDoDdd() {
        assertEquals(Optional.of("5511999101739"), TelefoneUtil.paraWhatsApp("011 99910-1739"));
    }

    @Test
    void celularAntigoGanhaNonoDigito() {
        assertEquals(Optional.of("5511999101739"), TelefoneUtil.paraWhatsApp("(11) 9910-1739"));
    }

    @Test
    void ddd55NaoEConfundidoComCodigoDoPais() {
        assertEquals(Optional.of("55" + "55" + "999101739"), TelefoneUtil.paraWhatsApp("(55) 99910-1739"));
    }

    @Test
    void telefoneFixoFicaComoEsta() {
        assertEquals(Optional.of("551147001234"), TelefoneUtil.paraWhatsApp("(11) 4700-1234"));
    }

    @Test
    void invalidos() {
        assertEquals(Optional.empty(), TelefoneUtil.paraWhatsApp(null));
        assertEquals(Optional.empty(), TelefoneUtil.paraWhatsApp(""));
        assertEquals(Optional.empty(), TelefoneUtil.paraWhatsApp("99910-1739"));
        assertEquals(Optional.empty(), TelefoneUtil.paraWhatsApp("(11) 89910-17390"));
        assertEquals(Optional.empty(), TelefoneUtil.paraWhatsApp("(11) 89999-9999"));
    }

    @Test
    void formatar() {
        assertEquals("(11) 99910-1739", TelefoneUtil.formatar("5511999101739"));
        assertEquals("(11) 4700-1234", TelefoneUtil.formatar("551147001234"));
    }
}
