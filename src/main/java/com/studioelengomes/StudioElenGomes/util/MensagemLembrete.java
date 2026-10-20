package com.studioelengomes.StudioElenGomes.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class MensagemLembrete {

    public static final String PADRAO_HORARIO = "Olá, {nome}! Passando para lembrar do seu horário {quando} às {hora} "
            + "({servico}) no {studio}. Se precisar remarcar, é só me avisar por aqui.";

    public static final String PADRAO_RETORNO = "Oi, {nome}! Já faz um tempinho desde o seu atendimento {servico} no {studio}. "
            + "Que tal já agendar o seu retorno? É só me responder por aqui.";

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private MensagemLembrete() {
    }

    public static String primeiroNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            return "tudo bem";
        }
        return nomeCompleto.trim().split("\\s+")[0];
    }

    public static String quando(LocalDate dia, LocalDate hoje) {
        if (dia.equals(hoje)) {
            return "hoje";
        }
        if (dia.equals(hoje.plusDays(1))) {
            return "amanhã";
        }
        return "dia " + dia.format(DIA_MES);
    }

    public static String horario(String modelo, String nomeCliente, String servico,
            LocalDateTime dataHora, LocalDate hoje, String studio) {
        return modelo
                .replace("{nome}", primeiroNome(nomeCliente))
                .replace("{quando}", quando(dataHora.toLocalDate(), hoje))
                .replace("{hora}", dataHora.format(HORA))
                .replace("{servico}", servico)
                .replace("{studio}", studio);
    }

    public static String retorno(String modelo, String nomeCliente, String servico, String studio) {
        return modelo
                .replace("{nome}", primeiroNome(nomeCliente))
                .replace("{servico}", servico)
                .replace("{studio}", studio);
    }

    public static String linkWhatsApp(String telefoneInternacional, String mensagem) {
        String texto = URLEncoder.encode(mensagem, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + telefoneInternacional + "?text=" + texto;
    }
}
