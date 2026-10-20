package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.dto.LembreteWhatsApp;
import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.model.Notificacao;
import com.studioelengomes.StudioElenGomes.model.Notificacao.StatusNotificacao;
import com.studioelengomes.StudioElenGomes.model.Notificacao.TipoNotificacao;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import com.studioelengomes.StudioElenGomes.repository.NotificacaoRepository;
import com.studioelengomes.StudioElenGomes.util.MensagemLembrete;
import com.studioelengomes.StudioElenGomes.util.TelefoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LembreteWhatsAppService {

    public static final String CANAL = "WHATSAPP";

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private static final int DIAS_ATRASO_MAXIMO_RETORNO = 30;

    private static final int DIAS_HISTORICO_RETORNO = 400;

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final Environment env;

    @Value("${studio.nome}")
    private String studioNome;

    @Transactional(readOnly = true)
    public List<LembreteWhatsApp> listarHorarios() {
        LocalDateTime agora = LocalDateTime.now(ZONA);
        LocalDateTime fim = agora.toLocalDate().plusDays(1).atTime(23, 59, 59);

        return agendamentoRepository.findByDataHoraBetweenOrderByDataHoraAsc(agora, fim).stream()
                .filter(a -> a.getStatus() == Agendamento.Status.AGENDADO
                        || a.getStatus() == Agendamento.Status.CONFIRMADO)
                .filter(a -> !jaEnviado(a.getId(), TipoNotificacao.LEMBRETE_24H))
                .map(a -> montar(a, TipoNotificacao.LEMBRETE_24H, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LembreteWhatsApp> listarRetornos() {
        LocalDateTime agora = LocalDateTime.now(ZONA);
        List<Agendamento.Status> ativos = List.of(Agendamento.Status.AGENDADO,
                Agendamento.Status.CONFIRMADO, Agendamento.Status.CONCLUIDO);

        return agendamentoRepository.findByStatusAndDataHoraBetweenOrderByDataHoraAsc(
                Agendamento.Status.CONCLUIDO, agora.minusDays(DIAS_HISTORICO_RETORNO), agora).stream()
                .filter(a -> Boolean.TRUE.equals(a.getServico().getRequerRetorno()))
                .filter(a -> a.getServico().getDiasParaRetorno() != null
                        && a.getServico().getDiasParaRetorno() > 0)
                .filter(a -> {
                    LocalDateTime prazo = a.getDataHora().plusDays(a.getServico().getDiasParaRetorno());
                    return !prazo.isAfter(agora)
                            && !prazo.isBefore(agora.minusDays(DIAS_ATRASO_MAXIMO_RETORNO));
                })
                .filter(a -> !jaEnviado(a.getId(), TipoNotificacao.LEMBRETE_RETORNO))
                .filter(a -> !agendamentoRepository.existsByClienteIdAndDataHoraAfterAndStatusIn(
                        a.getCliente().getId(), a.getDataHora(), ativos))
                .map(a -> montar(a, TipoNotificacao.LEMBRETE_RETORNO, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LembreteWhatsApp> listarEnviadosHoje() {
        LocalDate hoje = LocalDate.now(ZONA);
        return notificacaoRepository.findByCanalAndStatusAndEnviadaEmBetweenOrderByEnviadaEmDesc(
                CANAL, StatusNotificacao.ENVIADA, hoje.atStartOfDay(), hoje.atTime(23, 59, 59)).stream()
                .map(n -> montar(n.getAgendamento(), n.getTipo(), n.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public int contarPendentes() {
        return listarHorarios().size() + listarRetornos().size();
    }

    @Transactional
    public void marcarEnviado(String agendamentoId, TipoNotificacao tipo) {
        if (tipo != TipoNotificacao.LEMBRETE_24H && tipo != TipoNotificacao.LEMBRETE_RETORNO) {
            throw new IllegalArgumentException("Tipo de lembrete inválido: " + tipo);
        }
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        if (jaEnviado(agendamentoId, tipo)) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now(ZONA);
        notificacaoRepository.save(Notificacao.builder()
                .agendamento(agendamento)
                .cliente(agendamento.getCliente())
                .tipo(tipo)
                .status(StatusNotificacao.ENVIADA)
                .canal(CANAL)
                .mensagem("Lembrete aberto no WhatsApp (link wa.me)")
                .criadaEm(agora)
                .enviadaEm(agora)
                .build());
    }

    @Transactional
    public void desfazer(Long notificacaoId) {
        notificacaoRepository.findById(notificacaoId)
                .filter(n -> CANAL.equals(n.getCanal()))
                .ifPresent(notificacaoRepository::delete);
    }

    private boolean jaEnviado(String agendamentoId, TipoNotificacao tipo) {
        return notificacaoRepository.existsByAgendamentoIdAndTipoAndCanalAndStatus(
                agendamentoId, tipo, CANAL, StatusNotificacao.ENVIADA);
    }

    private LembreteWhatsApp montar(Agendamento a, TipoNotificacao tipo, Long notificacaoId) {
        String nome = a.getCliente().getNome();
        String servico = a.getServico().getNome();

        String mensagem = tipo == TipoNotificacao.LEMBRETE_RETORNO
                ? MensagemLembrete.retorno(
                        env.getProperty("studio.whatsapp.mensagem-retorno", MensagemLembrete.PADRAO_RETORNO),
                        nome, servico, studioNome)
                : MensagemLembrete.horario(
                        env.getProperty("studio.whatsapp.mensagem-horario", MensagemLembrete.PADRAO_HORARIO),
                        nome, servico, a.getDataHora(), LocalDate.now(ZONA), studioNome);

        Optional<String> telefone = TelefoneUtil.paraWhatsApp(a.getCliente().getTelefone());

        return LembreteWhatsApp.builder()
                .agendamentoId(a.getId())
                .notificacaoId(notificacaoId)
                .tipo(tipo.name())
                .clienteId(a.getCliente().getId())
                .clienteNome(nome)
                .telefone(telefone.map(TelefoneUtil::formatar).orElse(a.getCliente().getTelefone()))
                .servicoNome(servico)
                .dataHora(a.getDataHora())
                .mensagem(mensagem)
                .link(telefone.map(t -> MensagemLembrete.linkWhatsApp(t, mensagem)).orElse(null))
                .build();
    }
}
