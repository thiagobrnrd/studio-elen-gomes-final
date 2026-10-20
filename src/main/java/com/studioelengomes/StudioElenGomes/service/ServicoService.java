package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.exception.NaoEncontradoException;
import com.studioelengomes.StudioElenGomes.model.Servico;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import com.studioelengomes.StudioElenGomes.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public List<Servico> listarTodos() {
        return servicoRepository.findAll(Sort.by("nome").ascending());
    }

    @Transactional(readOnly = true)
    public List<Servico> listarAtivos() {
        return servicoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Servico> listarOnline() {
        return servicoRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(s -> Boolean.TRUE.equals(s.getOnline())).toList();
    }

    @Transactional(readOnly = true)
    public Servico buscarPorId(String id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Serviço não encontrado"));
    }

    @Transactional
    public Servico criar(Servico servico) {
        servico.setId(null);
        normalizar(servico);
        return servicoRepository.save(servico);
    }

    @Transactional
    public Servico atualizar(String id, Servico dados) {
        Servico atual = buscarPorId(id);
        normalizar(dados);
        atual.setNome(dados.getNome());
        atual.setDescricao(dados.getDescricao());
        atual.setDuracaoMin(dados.getDuracaoMin());
        atual.setPreco(dados.getPreco());
        atual.setAtivo(dados.getAtivo());
        atual.setRequerRetorno(dados.getRequerRetorno());
        atual.setDiasParaRetorno(dados.getDiasParaRetorno());
        atual.setOnline(dados.getOnline());
        atual.setPrecoAPartir(dados.getPrecoAPartir());
        return servicoRepository.save(atual);
    }

    @Transactional
    public Servico alternarAtivo(String id) {
        Servico servico = buscarPorId(id);
        servico.setAtivo(!Boolean.TRUE.equals(servico.getAtivo()));
        return servicoRepository.save(servico);
    }

    @Transactional
    public void deletar(String id) {
        Servico s = buscarPorId(id);
        if (agendamentoRepository.existsByServicoId(id)) {
            throw new IllegalStateException("Este serviço já foi usado em atendimentos. Desative-o em vez de excluir.");
        }
        servicoRepository.delete(s);
    }

    private void normalizar(Servico s) {
        if (s.getNome() != null) s.setNome(s.getNome().trim());
        if (s.getAtivo() == null) s.setAtivo(true);
        if (s.getRequerRetorno() == null) s.setRequerRetorno(false);
        if (s.getOnline() == null) s.setOnline(true);
        if (s.getPrecoAPartir() == null) s.setPrecoAPartir(true);
        if (!s.getRequerRetorno()) s.setDiasParaRetorno(null);
        if (s.getRequerRetorno() && (s.getDiasParaRetorno() == null || s.getDiasParaRetorno() <= 0)) {
            throw new IllegalArgumentException("Informe em quantos dias a cliente deve voltar.");
        }
    }
}
