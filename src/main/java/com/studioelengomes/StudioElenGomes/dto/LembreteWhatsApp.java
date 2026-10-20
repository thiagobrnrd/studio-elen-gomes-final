package com.studioelengomes.StudioElenGomes.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LembreteWhatsApp {

    private final String agendamentoId;

    private final Long notificacaoId;

    private final String tipo;

    private final String clienteId;
    private final String clienteNome;

    private final String telefone;

    private final String servicoNome;
    private final LocalDateTime dataHora;
    private final String mensagem;

    private final String link;
}
