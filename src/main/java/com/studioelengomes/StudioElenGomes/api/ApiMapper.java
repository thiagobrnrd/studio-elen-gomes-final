package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.api.dto.*;
import com.studioelengomes.StudioElenGomes.model.*;
import com.studioelengomes.StudioElenGomes.util.TelefoneUtil;

public final class ApiMapper {
    private ApiMapper() {}

    public static ClienteDto cliente(Cliente c, long total) {
        return new ClienteDto(c.getId(), c.getNome(), c.getTelefone(), c.getEmail(), c.getDataNascimento(), total,
                TelefoneUtil.paraWhatsApp(c.getTelefone()).orElse(null));
    }

    public static ServicoDto servico(Servico s) {
        return new ServicoDto(s.getId(), s.getNome(), s.getDescricao(), s.getDuracaoMin(), s.getPreco(), s.getAtivo(),
                s.getRequerRetorno(), s.getDiasParaRetorno());
    }

    public static AgendamentoDto agendamento(Agendamento a) {
        return agendamento(a, false);
    }

    public static AgendamentoDto agendamento(Agendamento a, boolean pago) {
        return new AgendamentoDto(a.getId(), a.getCliente().getId(), a.getCliente().getNome(), a.getCliente().getTelefone(),
                TelefoneUtil.paraWhatsApp(a.getCliente().getTelefone()).orElse(null),
                a.getServico().getId(), a.getServico().getNome(), a.getServico().getDuracaoMin(), a.getServico().getPreco(),
                a.getDataHora(), a.getStatus().name(), a.getObservacoes(), pago);
    }
}
