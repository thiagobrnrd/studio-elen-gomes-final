package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.api.dto.AgendamentoDto;
import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.service.PedidoOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Pedidos de agendamento online — tela "Pedidos" do painel da Elen. */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidosApiController {
    private final PedidoOnlineService service;

    public record Pedido(AgendamentoDto agendamento, boolean clienteNova) {}
    public record Lista(List<Pedido> pendentes, List<Pedido> recentes) {}
    public record Contagem(long pendentes, LocalDateTime ultimo) {}
    public record Recusa(String motivo) {}

    @GetMapping
    public Lista listar() {
        return new Lista(service.pendentes().stream().map(this::pedido).toList(),
                service.recentes().stream().map(this::pedido).toList());
    }

    @GetMapping("/contagem")
    public Contagem contagem() {
        return new Contagem(service.contarPendentes(), service.ultimoPedido().orElse(null));
    }

    @PostMapping("/{id}/aprovar")
    public AgendamentoDto aprovar(@PathVariable String id) {
        return ApiMapper.agendamento(service.aprovar(id));
    }

    @PostMapping("/{id}/recusar")
    public AgendamentoDto recusar(@PathVariable String id, @RequestBody(required = false) Recusa r) {
        return ApiMapper.agendamento(service.recusar(id, r == null ? null : r.motivo()));
    }

    private Pedido pedido(Agendamento a) {
        return new Pedido(ApiMapper.agendamento(a), service.clienteNova(a));
    }
}
