package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.exception.NaoEncontradoException;
import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.model.Cliente;
import com.studioelengomes.StudioElenGomes.model.Servico;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import com.studioelengomes.StudioElenGomes.repository.ClienteRepository;
import com.studioelengomes.StudioElenGomes.util.TelefoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Pedidos de agendamento feitos pela cliente na página pública (/agendar).
 * O pedido entra como SOLICITADO (horário já reservado) e a Elen aprova ou recusa.
 * Se a aprovação automática estiver ligada, entra direto como CONFIRMADO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoOnlineService {

    public static final String ORIGEM_ONLINE = "ONLINE";
    private static final int MAX_PEDIDOS_PENDENTES_POR_CLIENTE = 2;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoService servicoService;
    private final ConfiguracaoService configuracaoService;
    private final DisponibilidadeService disponibilidadeService;

    public record NovoPedido(String servicoId, LocalDateTime dataHora, String nome, String telefone,
                             String observacoes, boolean aceitePrivacidade) {}

    @Transactional
    public Agendamento criar(NovoPedido p) {
        StudioConfig c = configuracaoService.atual();
        if (!c.onlineAtivo()) {
            throw new IllegalStateException("O agendamento online está pausado no momento. Fale com o studio pelo WhatsApp.");
        }
        String nome = p.nome() == null ? "" : p.nome().trim().replaceAll("\\s+", " ");
        if (nome.length() < 3 || nome.length() > 120) {
            throw new IllegalArgumentException("Informe seu nome completo.");
        }
        String whatsapp = TelefoneUtil.paraWhatsApp(p.telefone())
                .orElseThrow(() -> new IllegalArgumentException("Informe um WhatsApp válido com DDD."));
        if (!p.aceitePrivacidade()) {
            throw new IllegalArgumentException("Para agendar, aceite a Política de Privacidade.");
        }
        if (p.servicoId() == null || p.dataHora() == null) {
            throw new IllegalArgumentException("Escolha o serviço e o horário.");
        }
        Servico servico = servicoService.buscarPorId(p.servicoId());
        if (!Boolean.TRUE.equals(servico.getAtivo()) || !Boolean.TRUE.equals(servico.getOnline())) {
            throw new IllegalArgumentException("Este serviço não está disponível para agendamento online.");
        }

        agendamentoRepository.travarAgenda();
        if (!disponibilidadeService.disponivelOnline(p.dataHora(), servico.getDuracaoMin())) {
            throw new IllegalStateException("Esse horário acabou de ser reservado. Por favor, escolha outro.");
        }

        Cliente cliente = encontrarOuCriarCliente(nome, whatsapp);
        if (cliente.getId() != null) {
            if (agendamentoRepository.existsByClienteIdAndDataHoraAndStatusNot(
                    cliente.getId(), p.dataHora(), Agendamento.Status.CANCELADO)) {
                throw new IllegalStateException("Você já tem um pedido para esse horário.");
            }
            long pendentes = agendamentoRepository.countByClienteIdAndStatusAndDataHoraAfter(
                    cliente.getId(), Agendamento.Status.SOLICITADO, LocalDateTime.now());
            if (pendentes >= MAX_PEDIDOS_PENDENTES_POR_CLIENTE) {
                throw new IllegalStateException("Você já tem pedidos aguardando confirmação. Aguarde o retorno do studio.");
            }
        }
        cliente.setPrivacidadeAceitaEm(LocalDateTime.now());
        cliente = clienteRepository.save(cliente);

        String obs = p.observacoes() == null ? null : p.observacoes().trim();
        if (obs != null && obs.isEmpty()) obs = null;
        if (obs != null && obs.length() > 500) obs = obs.substring(0, 500);

        Agendamento a = Agendamento.builder()
                .cliente(cliente)
                .servico(servico)
                .dataHora(p.dataHora())
                .observacoes(obs)
                .status(c.aprovacaoAutomatica() ? Agendamento.Status.CONFIRMADO : Agendamento.Status.SOLICITADO)
                .origem(ORIGEM_ONLINE)
                .token(novoToken())
                .build();
        a = agendamentoRepository.save(a);
        log.info("Pedido online: {} - {} em {}", cliente.getNome(), servico.getNome(), a.getDataHora());
        return a;
    }

    @Transactional(readOnly = true)
    public Agendamento buscarPorToken(String token) {
        return agendamentoRepository.findByToken(token)
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
    }

    public static boolean podeCancelar(Agendamento a) {
        return (a.getStatus() == Agendamento.Status.SOLICITADO
                || a.getStatus() == Agendamento.Status.AGENDADO
                || a.getStatus() == Agendamento.Status.CONFIRMADO)
                && a.getDataHora().isAfter(LocalDateTime.now());
    }

    @Transactional
    public Agendamento cancelarPelaCliente(String token) {
        Agendamento a = buscarPorToken(token);
        if (!podeCancelar(a)) {
            throw new IllegalStateException("Este horário não pode mais ser cancelado pelo link. Fale com o studio.");
        }
        a.setStatus(Agendamento.Status.CANCELADO);
        a.setMotivoCancelamento("Cancelado pela cliente");
        return agendamentoRepository.save(a);
    }

    // ─── Painel da Elen ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Agendamento> pendentes() {
        return agendamentoRepository.findByStatusOrderByDataHoraAsc(Agendamento.Status.SOLICITADO);
    }

    @Transactional(readOnly = true)
    public List<Agendamento> recentes() {
        return agendamentoRepository.findTop15ByOrigemAndStatusNotOrderByAtualizadoEmDesc(
                ORIGEM_ONLINE, Agendamento.Status.SOLICITADO);
    }

    @Transactional(readOnly = true)
    public long contarPendentes() {
        return agendamentoRepository.countByStatus(Agendamento.Status.SOLICITADO);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> ultimoPedido() {
        return Optional.ofNullable(agendamentoRepository.findFirstByStatusOrderByCriadoEmDesc(Agendamento.Status.SOLICITADO))
                .map(Agendamento::getCriadoEm);
    }

    @Transactional
    public Agendamento aprovar(String id) {
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
        if (a.getStatus() != Agendamento.Status.SOLICITADO) {
            throw new IllegalStateException("Este pedido já foi respondido.");
        }
        a.setStatus(Agendamento.Status.CONFIRMADO);
        return agendamentoRepository.save(a);
    }

    @Transactional
    public Agendamento recusar(String id, String motivo) {
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
        if (a.getStatus() != Agendamento.Status.SOLICITADO) {
            throw new IllegalStateException("Este pedido já foi respondido.");
        }
        a.setStatus(Agendamento.Status.CANCELADO);
        String m = motivo == null || motivo.isBlank() ? "Horário indisponível" : motivo.trim();
        a.setMotivoCancelamento(m.length() > 250 ? m.substring(0, 250) : m);
        return agendamentoRepository.save(a);
    }

    @Transactional(readOnly = true)
    public boolean clienteNova(Agendamento a) {
        return agendamentoRepository.countByClienteId(a.getCliente().getId()) <= 1;
    }

    // ─── internos ─────────────────────────────────────────────────────

    /** Procura a cliente pelo WhatsApp (independente de como foi digitado). Se não achar, prepara uma nova. */
    private Cliente encontrarOuCriarCliente(String nome, String whatsapp) {
        return clienteRepository.findAll().stream()
                .filter(c -> TelefoneUtil.paraWhatsApp(c.getTelefone()).map(whatsapp::equals).orElse(false))
                .findFirst()
                .orElseGet(() -> Cliente.builder().nome(nome).telefone(TelefoneUtil.formatar(whatsapp)).build());
    }

    private static String novoToken() {
        byte[] b = new byte[18];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
