package com.studioelengomes.StudioElenGomes.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Uma linha da tela de lembretes. */
@Getter
@Builder
public class LembreteWhatsApp {

    private final String agendamentoId;

    /** Preenchido só nos lembretes já enviados (usado para desfazer). */
    private final Long notificacaoId;

    /** LEMBRETE_24H ou LEMBRETE_RETORNO. */
    private final String tipo;

    private final String clienteId;
    private final String clienteNome;

    /** Telefone para exibir (formatado, ou como foi digitado se inválido). */
    private final String telefone;

    private final String servicoNome;
    private final LocalDateTime dataHora;
    private final String mensagem;

    /** Link wa.me, ou null se o telefone da cliente estiver incompleto. */
    private final String link;
}
