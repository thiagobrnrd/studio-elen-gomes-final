package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.exception.NaoEncontradoException;
import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.model.Notificacao;
import com.studioelengomes.StudioElenGomes.model.Remarcacao;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import com.studioelengomes.StudioElenGomes.repository.NotificacaoRepository;
import com.studioelengomes.StudioElenGomes.repository.PagamentoRepository;
import com.studioelengomes.StudioElenGomes.repository.RemarcacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private static final int JANELA_CONFLITO_HORAS = 12;

    private final AgendamentoRepository agendamentoRepository;
    private final RemarcacaoRepository remarcacaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final PagamentoRepository pagamentoRepository;

    @Transactional(readOnly = true)
    public List<Agendamento> listarTodos() {
        return agendamentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listarHoje() {
        LocalDate hoje = LocalDate.now();
        return agendamentoRepository.findByDataHoraBetweenOrderByDataHoraAsc(hoje.atStartOfDay(), hoje.atTime(23, 59, 59));
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listarPorCliente(String clienteId) {
        return agendamentoRepository.findByClienteIdOrderByDataHoraDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public Agendamento buscarPorId(String id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Agendamento não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByDataHoraBetweenOrderByDataHoraAsc(inicio, fim);
    }

    @Transactional(readOnly = true)
    public boolean temConflito(LocalDateTime inicio, int duracaoMin, String ignorarId) {
        LocalDateTime fim = inicio.plusMinutes(duracaoMin);
        return agendamentoRepository
                .findByDataHoraBetweenOrderByDataHoraAsc(inicio.minusHours(JANELA_CONFLITO_HORAS), fim)
                .stream()
                .filter(a -> !Objects.equals(a.getId(), ignorarId))
                .filter(a -> a.getStatus() != Agendamento.Status.CANCELADO)
                .anyMatch(a -> {
                    LocalDateTime aFim = a.getDataHora().plusMinutes(a.getServico().getDuracaoMin());
                    return a.getDataHora().isBefore(fim) && aFim.isAfter(inicio);
                });
    }

    @Transactional
    public Agendamento criar(Agendamento agendamento) {
        agendamento.setId(null);
        if (agendamento.getDataHora() == null) {
            throw new IllegalArgumentException("Informe a data e o horário.");
        }
        if (temConflito(agendamento.getDataHora(), agendamento.getServico().getDuracaoMin(), null)) {
            throw new IllegalArgumentException("Já existe um atendimento nesse horário.");
        }
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento remarcar(String id, LocalDateTime novaDataHora, String motivo) {
        if (novaDataHora == null) {
            throw new IllegalArgumentException("Escolha o novo horário.");
        }
        Agendamento a = buscarPorId(id);
        if (a.getStatus() == Agendamento.Status.CANCELADO || a.getStatus() == Agendamento.Status.CONCLUIDO) {
            throw new IllegalArgumentException("Só é possível remarcar atendimentos agendados ou confirmados.");
        }
        LocalDateTime antiga = a.getDataHora();
        if (novaDataHora.equals(antiga)) {
            throw new IllegalArgumentException("O novo horário é igual ao atual.");
        }
        if (temConflito(novaDataHora, a.getServico().getDuracaoMin(), a.getId())) {
            throw new IllegalArgumentException("Já existe um atendimento nesse horário.");
        }

        a.setDataHora(novaDataHora);
        a.setStatus(Agendamento.Status.AGENDADO);
        Agendamento salvo = agendamentoRepository.save(a);

        remarcacaoRepository.save(Remarcacao.builder()
                .agendamento(salvo)
                .dataHoraAnterior(antiga)
                .novaDataHora(novaDataHora)
                .motivo(motivo == null || motivo.isBlank() ? "Solicitação de remarcação" : motivo.trim())
                .build());

        notificacaoRepository.deleteByAgendamentoIdAndTipoAndCanal(
                salvo.getId(), Notificacao.TipoNotificacao.LEMBRETE_24H, LembreteWhatsAppService.CANAL);
        return salvo;
    }

    @Transactional
    public Agendamento alterarStatus(String id, Agendamento.Status novoStatus) {
        Agendamento agendamento = buscarPorId(id);
        if (agendamento.getStatus() == Agendamento.Status.CANCELADO && novoStatus != Agendamento.Status.CANCELADO
                && temConflito(agendamento.getDataHora(), agendamento.getServico().getDuracaoMin(), id)) {
            throw new IllegalArgumentException("Esse horário já foi ocupado por outro atendimento. Use \"Remarcar\".");
        }
        agendamento.setStatus(novoStatus);
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public void deletar(String id) {
        Agendamento a = buscarPorId(id);
        if (pagamentoRepository.existsByAgendamentoId(id)) {
            throw new IllegalStateException("Este atendimento já tem pagamento registrado e não pode ser excluído. Use \"Cancelar\".");
        }
        agendamentoRepository.delete(a);
    }

    @Transactional(readOnly = true)
    public long contarAgendamentosHoje() {
        return listarHoje().size();
    }

    @Transactional(readOnly = true)
    public long contarAgendamentosPendentes() {
        return agendamentoRepository.countByStatusAndDataHoraAfter(Agendamento.Status.AGENDADO, LocalDateTime.now());
    }
}
