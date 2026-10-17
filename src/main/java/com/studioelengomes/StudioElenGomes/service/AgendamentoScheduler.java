package com.studioelengomes.StudioElenGomes.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Os lembretes do Studio são preparados em tempo real pela Central de Lembretes
 * e abertos pela Elen através de links wa.me. Não há envio automático por e-mail.
 * Isso mantém o fluxo compatível com a decisão de não depender da API oficial
 * do WhatsApp neste momento.
 */
@Component
@Slf4j
public class AgendamentoScheduler {
    public AgendamentoScheduler() {
        log.info("Central de lembretes WhatsApp ativa — envio realizado via wa.me.");
    }
}
