package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.api.dto.AgendamentoDto;
import com.studioelengomes.StudioElenGomes.api.dto.RemarcacaoRequest;
import com.studioelengomes.StudioElenGomes.repository.RemarcacaoRepository;
import com.studioelengomes.StudioElenGomes.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos/{agendamentoId}/remarcacoes")
@RequiredArgsConstructor
public class RemarcacaoApiController {
    private final RemarcacaoRepository repository;
    private final AgendamentoService service;

    public record Item(Long id, LocalDateTime anterior, LocalDateTime nova, String motivo, LocalDateTime criadoEm) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<Item> listar(@PathVariable String agendamentoId) {
        return repository.findByAgendamentoIdOrderByCriadoEmDesc(agendamentoId).stream()
                .map(r -> new Item(r.getId(), r.getDataHoraAnterior(), r.getNovaDataHora(), r.getMotivo(), r.getCriadoEm()))
                .toList();
    }

    @PostMapping
    public AgendamentoDto remarcar(@PathVariable String agendamentoId, @Valid @RequestBody RemarcacaoRequest request) {
        return ApiMapper.agendamento(service.remarcar(agendamentoId, request.novaDataHora(), request.motivo()));
    }
}
