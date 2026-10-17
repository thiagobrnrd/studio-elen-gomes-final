package com.studioelengomes.StudioElenGomes.api.dto;
import java.math.BigDecimal;
import java.util.List;
public record DashboardDto(long atendimentosHoje, long clientes, long servicosAtivos, long pendentes, long lembretesPendentes, BigDecimal faturamentoHoje, List<AgendamentoDto> proximos) {}
