package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.api.dto.*;
import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.repository.PagamentoRepository;
import com.studioelengomes.StudioElenGomes.service.AgendamentoService;
import com.studioelengomes.StudioElenGomes.service.ClienteService;
import com.studioelengomes.StudioElenGomes.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoApiController {
    private final AgendamentoService service;
    private final ClienteService clientes;
    private final ServicoService servicos;
    private final PagamentoRepository pagamentos;

    @GetMapping
    public List<AgendamentoDto> listar(@RequestParam(required = false) String data,
                                       @RequestParam(required = false) String clienteId) {
        List<Agendamento> list;
        if (clienteId != null && !clienteId.isBlank()) {
            list = service.listarPorCliente(clienteId);
        } else if (data == null || data.isBlank()) {
            list = service.listarTodos().stream().sorted(Comparator.comparing(Agendamento::getDataHora)).toList();
        } else {
            LocalDate d = LocalDate.parse(data);
            list = service.listarPorPeriodo(d.atStartOfDay(), d.atTime(23, 59, 59));
        }
        return list.stream().map(this::dto).toList();
    }

    @GetMapping("/{id}")
    public AgendamentoDto buscar(@PathVariable String id) {
        return dto(service.buscarPorId(id));
    }

    @PostMapping
    public AgendamentoDto criar(@Valid @RequestBody AgendamentoRequest r) {
        var servico = servicos.buscarPorId(r.servicoId());
        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new IllegalArgumentException("Este serviço está desativado.");
        }
        var a = Agendamento.builder()
                .cliente(clientes.buscarPorId(r.clienteId()))
                .servico(servico)
                .dataHora(r.dataHora())
                .observacoes(r.observacoes() == null || r.observacoes().isBlank() ? null : r.observacoes().trim())
                .status(Agendamento.Status.AGENDADO)
                .build();
        return dto(service.criar(a));
    }

    @PatchMapping("/{id}/status")
    public AgendamentoDto status(@PathVariable String id, @RequestParam Agendamento.Status value) {
        return dto(service.alterarStatus(id, value));
    }

    @PostMapping("/{id}/reschedule")
    public AgendamentoDto remarcar(@PathVariable String id, @Valid @RequestBody RemarcacaoRequest r) {
        return dto(service.remarcar(id, r.novaDataHora(), r.motivo()));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id) {
        service.deletar(id);
    }

    private AgendamentoDto dto(Agendamento a) {
        return ApiMapper.agendamento(a, pagamentos.existsByAgendamentoId(a.getId()));
    }
}
