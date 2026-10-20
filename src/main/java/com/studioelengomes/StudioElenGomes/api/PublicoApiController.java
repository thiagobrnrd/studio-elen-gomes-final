package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.service.*;
import com.studioelengomes.StudioElenGomes.util.MensagemLembrete;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rotas abertas (sem login) usadas pela página pública de agendamento /agendar.
 * Só expõem o necessário: serviços online, horários livres e o próprio pedido (pelo token).
 */
@RestController
@RequestMapping("/api/publico")
@RequiredArgsConstructor
public class PublicoApiController {

    private final ConfiguracaoService configuracaoService;
    private final ServicoService servicoService;
    private final DisponibilidadeService disponibilidadeService;
    private final PedidoOnlineService pedidoService;
    private final LimiteRequisicoes limite;

    @Value("${studio.nome:Studio Elen Gomes}")
    private String studioNome;

    public record ServicoPublico(String id, String nome, String descricao, Integer duracaoMin, BigDecimal preco, boolean precoAPartir) {}
    public record Info(String nome, boolean onlineAtivo, String mensagem, String endereco, String instagram,
                       String whatsapp, String politica, String abertura, String fechamento,
                       List<Integer> diasFuncionamento, List<ServicoPublico> servicos) {}
    public record PedidoRequest(String servicoId, LocalDateTime dataHora, String nome, String telefone,
                                String observacoes, String hp, Boolean aceitePrivacidade) {}
    public record PedidoPublico(String token, String status, String clienteNome, String servicoNome,
                                Integer duracaoMin, BigDecimal valor, boolean precoAPartir, LocalDateTime dataHora, String observacoes,
                                String motivo, boolean podeCancelar, String studioNome, String studioWhatsapp,
                                String endereco, String politica) {}

    @GetMapping("/info")
    public Info info() {
        StudioConfig c = configuracaoService.atual();
        List<ServicoPublico> servicos = c.onlineAtivo()
                ? servicoService.listarOnline().stream()
                    .map(s -> new ServicoPublico(s.getId(), s.getNome(), s.getDescricao(), s.getDuracaoMin(), s.getPreco(), Boolean.TRUE.equals(s.getPrecoAPartir())))
                    .toList()
                : List.of();
        return new Info(studioNome, c.onlineAtivo(), c.mensagem(), c.endereco(), c.instagram(), c.whatsapp(),
                c.politica(), c.abertura().toString(), c.fechamento().toString(),
                c.diasFuncionamento().stream().map(DayOfWeek::getValue).sorted().toList(), servicos);
    }

    @GetMapping("/dias")
    public List<DisponibilidadeService.Dia> dias(@RequestParam String servicoId) {
        var s = servicoOnline(servicoId);
        return disponibilidadeService.diasOnline(s.getDuracaoMin());
    }

    @GetMapping("/horarios")
    public List<DisponibilidadeService.Slot> horarios(@RequestParam String servicoId, @RequestParam String data) {
        var s = servicoOnline(servicoId);
        return disponibilidadeService.horarios(LocalDate.parse(data), s.getDuracaoMin(), null, true);
    }

    @PostMapping("/agendamentos")
    public PedidoPublico pedir(@RequestBody PedidoRequest r, HttpServletRequest http) {
        if (r.hp() != null && !r.hp().isBlank()) {
            // campo invisível preenchido = robô
            throw new IllegalArgumentException("Não foi possível enviar o pedido.");
        }
        if (!limite.permitir("pedido:" + ip(http), 6, 60 * 60 * 1000L)) {
            throw new IllegalStateException("Muitos pedidos em pouco tempo. Tente novamente mais tarde ou fale com o studio pelo WhatsApp.");
        }
        Agendamento a = pedidoService.criar(new PedidoOnlineService.NovoPedido(
                r.servicoId(), r.dataHora(), r.nome(), r.telefone(), r.observacoes(), Boolean.TRUE.equals(r.aceitePrivacidade())));
        return dto(a);
    }

    @GetMapping("/pedidos/{token}")
    public PedidoPublico pedido(@PathVariable String token, HttpServletRequest http) {
        if (!limite.permitir("consulta:" + ip(http), 120, 60 * 60 * 1000L)) {
            throw new IllegalStateException("Muitas consultas. Tente novamente em instantes.");
        }
        return dto(pedidoService.buscarPorToken(token));
    }

    @PostMapping("/pedidos/{token}/cancelar")
    public PedidoPublico cancelar(@PathVariable String token) {
        return dto(pedidoService.cancelarPelaCliente(token));
    }

    private com.studioelengomes.StudioElenGomes.model.Servico servicoOnline(String id) {
        var s = servicoService.buscarPorId(id);
        if (!Boolean.TRUE.equals(s.getAtivo()) || !Boolean.TRUE.equals(s.getOnline())) {
            throw new IllegalArgumentException("Serviço indisponível para agendamento online.");
        }
        return s;
    }

    private PedidoPublico dto(Agendamento a) {
        StudioConfig c = configuracaoService.atual();
        return new PedidoPublico(a.getToken(), a.getStatus().name(),
                MensagemLembrete.primeiroNome(a.getCliente().getNome()),
                a.getServico().getNome(), a.getServico().getDuracaoMin(), a.getServico().getPreco(), Boolean.TRUE.equals(a.getServico().getPrecoAPartir()),
                a.getDataHora(), a.getObservacoes(), a.getMotivoCancelamento(),
                PedidoOnlineService.podeCancelar(a), studioNome, c.whatsapp(), c.endereco(), c.politica());
    }

    private static String ip(HttpServletRequest r) {
        String f = r.getHeader("X-Forwarded-For");
        if (f != null && !f.isBlank()) return f.split(",")[0].trim();
        return r.getRemoteAddr();
    }
}
