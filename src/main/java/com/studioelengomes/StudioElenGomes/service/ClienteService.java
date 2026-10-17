package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.exception.NaoEncontradoException;
import com.studioelengomes.StudioElenGomes.model.Cliente;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import com.studioelengomes.StudioElenGomes.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll(Sort.by("nome").ascending());
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado"));
    }

    @Transactional
    public Cliente criar(Cliente cliente) {
        cliente.setId(null);
        normalizar(cliente);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente atualizar(String id, Cliente dados) {
        Cliente atual = buscarPorId(id);
        normalizar(dados);
        atual.setNome(dados.getNome());
        atual.setTelefone(dados.getTelefone());
        atual.setEmail(dados.getEmail());
        atual.setDataNascimento(dados.getDataNascimento());
        return clienteRepository.save(atual);
    }

    @Transactional
    public void deletar(String id) {
        Cliente c = buscarPorId(id);
        if (agendamentoRepository.existsByClienteId(id)) {
            throw new IllegalStateException("Esta cliente tem atendimentos no histórico e não pode ser excluída.");
        }
        clienteRepository.delete(c);
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim());
    }

    @Transactional(readOnly = true)
    public long contar() {
        return clienteRepository.count();
    }

    private void normalizar(Cliente c) {
        if (c.getNome() != null) c.setNome(c.getNome().trim());
        if (c.getTelefone() != null) c.setTelefone(c.getTelefone().trim());
        if (c.getEmail() != null && c.getEmail().isBlank()) c.setEmail(null);
        if (c.getEmail() != null) c.setEmail(c.getEmail().trim());
    }
}
