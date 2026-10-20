package com.studioelengomes.StudioElenGomes.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AgendamentoScheduler {
    public AgendamentoScheduler() {
        log.info("Central de lembretes WhatsApp ativa — envio realizado via wa.me.");
    }
}
