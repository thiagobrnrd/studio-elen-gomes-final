package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.api.dto.ServicoDto;
import com.studioelengomes.StudioElenGomes.model.Servico;
import com.studioelengomes.StudioElenGomes.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicoApiController {
    private final ServicoService service;

    @GetMapping
    public List<ServicoDto> listar(@RequestParam(defaultValue = "false") boolean apenasAtivos) {
        return (apenasAtivos ? service.listarAtivos() : service.listarTodos()).stream().map(ApiMapper::servico).toList();
    }

    @PostMapping
    public ServicoDto criar(@Valid @RequestBody Servico s) {
        return ApiMapper.servico(service.criar(s));
    }

    @PutMapping("/{id}")
    public ServicoDto editar(@PathVariable String id, @Valid @RequestBody Servico s) {
        return ApiMapper.servico(service.atualizar(id, s));
    }

    @PatchMapping("/{id}/ativo")
    public ServicoDto alternar(@PathVariable String id) {
        return ApiMapper.servico(service.alternarAtivo(id));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id) {
        service.deletar(id);
    }
}
