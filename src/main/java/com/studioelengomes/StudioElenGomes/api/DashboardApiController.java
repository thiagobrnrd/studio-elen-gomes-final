package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.api.dto.DashboardDto;
import com.studioelengomes.StudioElenGomes.repository.PagamentoRepository;
import com.studioelengomes.StudioElenGomes.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {
    private final AgendamentoService ag;
    private final ClienteService cl;
    private final ServicoService sv;
    private final LembreteWhatsAppService lm;
    private final PagamentoRepository pagamentos;

    @GetMapping
    public DashboardDto dashboard() {
        var hoje = ag.listarHoje();
        LocalDate d = LocalDate.now();
        var faturamento = pagamentos.totalPorPeriodo(d.atStartOfDay(), d.atTime(23, 59, 59));
        return new DashboardDto(
                hoje.stream().filter(a -> a.getStatus() != com.studioelengomes.StudioElenGomes.model.Agendamento.Status.CANCELADO).count(),
                cl.contar(),
                sv.listarAtivos().size(),
                ag.contarAgendamentosPendentes(),
                lm.contarPendentes(),
                faturamento,
                hoje.stream().map(ApiMapper::agendamento).toList());
    }
}
