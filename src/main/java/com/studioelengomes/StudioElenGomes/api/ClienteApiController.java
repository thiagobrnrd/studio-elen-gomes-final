package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.api.dto.ClienteDto;
import com.studioelengomes.StudioElenGomes.model.Cliente;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import com.studioelengomes.StudioElenGomes.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteApiController {
    private final ClienteService service;
    private final AgendamentoRepository agendamentoRepository;

    @GetMapping
    public List<ClienteDto> listar(@RequestParam(required = false) String q) {
        List<Cliente> list = (q == null || q.isBlank()) ? service.listarTodos() : service.buscarPorNome(q);
        return list.stream().map(this::dto).toList();
    }

    @GetMapping("/{id}")
    public ClienteDto buscar(@PathVariable String id) {
        return dto(service.buscarPorId(id));
    }

    @PostMapping
    public ClienteDto criar(@Valid @RequestBody Cliente c) {
        return dto(service.criar(c));
    }

    @PutMapping("/{id}")
    public ClienteDto editar(@PathVariable String id, @Valid @RequestBody Cliente c) {
        return dto(service.atualizar(id, c));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id) {
        service.deletar(id);
    }

    private ClienteDto dto(Cliente c) {
        return ApiMapper.cliente(c, agendamentoRepository.countByClienteId(c.getId()));
    }
}
