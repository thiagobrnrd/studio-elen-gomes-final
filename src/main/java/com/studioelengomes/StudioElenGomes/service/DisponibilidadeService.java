package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calcula horários livres a partir das configurações do studio
 * (expediente, intervalo de almoço, dias de funcionamento, antecedência).
 *
 * Busca os atendimentos do período uma única vez e faz as contas em memória,
 * para a página pública responder rápido mesmo consultando 30 dias.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private final AgendamentoRepository agendamentoRepository;
    private final ConfiguracaoService configuracaoService;

    public record Slot(LocalDateTime dataHora, String hora) {}
    public record Dia(LocalDate data, int vagas) {}

    /** Horários livres de um dia. publico=true aplica as regras da página de agendamento online. */
    @Transactional(readOnly = true)
    public List<Slot> horarios(LocalDate dia, int duracao, String ignorarId, boolean publico) {
        StudioConfig c = configuracaoService.atual();
        if (publico && !diaAbertoParaOnline(c, dia)) return List.of();
        List<Agendamento> ocupados = ocupados(dia.atStartOfDay().minusHours(12), dia.plusDays(1).atStartOfDay());
        return calcular(c, dia, duracao, ignorarId, publico, ocupados, LocalDateTime.now());
    }

    /** Próximos dias em que o studio atende online, com a quantidade de horários livres. */
    @Transactional(readOnly = true)
    public List<Dia> diasOnline(int duracao) {
        StudioConfig c = configuracaoService.atual();
        LocalDate hoje = LocalDate.now();
        LocalDate fim = hoje.plusDays(c.diasMaximos());
        List<Agendamento> ocupados = ocupados(hoje.atStartOfDay().minusHours(12), fim.plusDays(1).atStartOfDay());
        LocalDateTime agora = LocalDateTime.now();
        List<Dia> dias = new ArrayList<>();
        for (LocalDate d = hoje; !d.isAfter(fim); d = d.plusDays(1)) {
            if (!c.diasFuncionamento().contains(d.getDayOfWeek())) continue;
            dias.add(new Dia(d, calcular(c, d, duracao, null, true, ocupados, agora).size()));
        }
        return dias;
    }

    @Transactional(readOnly = true)
    public boolean disponivelOnline(LocalDateTime dataHora, int duracao) {
        return horarios(dataHora.toLocalDate(), duracao, null, true).stream()
                .anyMatch(s -> s.dataHora().equals(dataHora));
    }

    private boolean diaAbertoParaOnline(StudioConfig c, LocalDate dia) {
        LocalDate hoje = LocalDate.now();
        return c.diasFuncionamento().contains(dia.getDayOfWeek())
                && !dia.isBefore(hoje)
                && !dia.isAfter(hoje.plusDays(c.diasMaximos()));
    }

    private List<Agendamento> ocupados(LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByDataHoraBetweenOrderByDataHoraAsc(inicio, fim).stream()
                .filter(a -> a.getStatus() != Agendamento.Status.CANCELADO)
                .toList();
    }

    private List<Slot> calcular(StudioConfig c, LocalDate dia, int duracao, String ignorarId, boolean publico,
                                List<Agendamento> ocupados, LocalDateTime agora) {
        LocalDateTime limite = publico ? agora.plusHours(c.antecedenciaMinHoras()) : agora.minusMinutes(1);
        List<Slot> slots = new ArrayList<>();
        LocalTime t = c.abertura();
        while (!t.plusMinutes(duracao).isAfter(c.fechamento()) && !t.plusMinutes(duracao).isBefore(t)) {
            LocalDateTime ini = LocalDateTime.of(dia, t);
            LocalDateTime fim = ini.plusMinutes(duracao);
            LocalTime tFim = t.plusMinutes(duracao);
            boolean noAlmoco = c.temAlmoco() && t.isBefore(c.almocoFim()) && tFim.isAfter(c.almocoInicio());
            if (!ini.isBefore(limite) && !noAlmoco) {
                boolean conflito = ocupados.stream()
                        .filter(a -> !Objects.equals(a.getId(), ignorarId))
                        .anyMatch(a -> a.getDataHora().isBefore(fim)
                                && a.getDataHora().plusMinutes(a.getServico().getDuracaoMin()).isAfter(ini));
                if (!conflito) slots.add(new Slot(ini, t.toString()));
            }
            LocalTime prox = t.plusMinutes(c.intervaloMinutos());
            if (!prox.isAfter(t)) break; // virou a meia-noite
            t = prox;
        }
        return slots;
    }
}
