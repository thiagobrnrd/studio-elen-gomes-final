package com.studioelengomes.StudioElenGomes.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensagemLembreteTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 1);

    @Test
    void horarioDeAmanha() {
        String msg = MensagemLembrete.horario(MensagemLembrete.PADRAO_HORARIO, "Maria Silva", "Escova",
                LocalDateTime.of(2026, 10, 2, 14, 0), HOJE, "Studio Elen Gomes");
        assertEquals("Olá, Maria! Passando para lembrar do seu horário amanhã às 14:00 (Escova) "
                + "no Studio Elen Gomes. Se precisar remarcar, é só me avisar por aqui. 💛", msg);
    }

    @Test
    void quandoHojeAmanhaOuData() {
        assertEquals("hoje", MensagemLembrete.quando(HOJE, HOJE));
        assertEquals("amanhã", MensagemLembrete.quando(HOJE.plusDays(1), HOJE));
        assertEquals("dia 05/10", MensagemLembrete.quando(LocalDate.of(2026, 10, 5), HOJE));
    }

    @Test
    void retorno() {
        String msg = MensagemLembrete.retorno(MensagemLembrete.PADRAO_RETORNO, "Ana Costa", "Mechas",
                "Studio Elen Gomes");
        assertTrue(msg.startsWith("Oi, Ana! Já faz um tempinho desde o seu Mechas"));
    }

    @Test
    void linkCodificaAcentosEEspacos() {
        String link = MensagemLembrete.linkWhatsApp("5511999101739", "Olá, Maria! 💛");
        assertEquals("https://wa.me/5511999101739?text=Ol%C3%A1%2C%20Maria%21%20%F0%9F%92%9B", link);
    }

    @Test
    void nomeVazioNaoQuebra() {
        assertEquals("tudo bem", MensagemLembrete.primeiroNome("  "));
    }
}
