package com.studioelengomes.StudioElenGomes.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record StudioConfig(
        boolean onlineAtivo,
        boolean aprovacaoAutomatica,
        Set<DayOfWeek> diasFuncionamento,
        LocalTime abertura,
        LocalTime fechamento,
        LocalTime almocoInicio,
        LocalTime almocoFim,
        int intervaloMinutos,
        int antecedenciaMinHoras,
        int diasMaximos,
        String endereco,
        String instagram,
        String whatsapp,
        String politica,
        String mensagem
) {
    public boolean temAlmoco() {
        return almocoInicio != null && almocoFim != null && almocoFim.isAfter(almocoInicio);
    }
}
