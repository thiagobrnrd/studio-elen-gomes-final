package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.model.Pagamento;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import com.studioelengomes.StudioElenGomes.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional
    public Pagamento registrar(String agendamentoId, Pagamento pagamento) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        if (agendamento.getStatus() == Agendamento.Status.CANCELADO) {
            throw new IllegalArgumentException("Não é possível registrar pagamento de um atendimento cancelado.");
        }
        if (pagamento.getValor() == null || pagamento.getValor().signum() < 0) {
            throw new IllegalArgumentException("Informe um valor válido.");
        }

        agendamento.setStatus(Agendamento.Status.CONCLUIDO);
        agendamentoRepository.save(agendamento);

        pagamento.setAgendamento(agendamento);
        pagamento.setPagoEm(LocalDateTime.now());
        return pagamentoRepository.save(pagamento);
    }

    public boolean jaFoiPago(String agendamentoId) {
        return pagamentoRepository.existsByAgendamentoId(agendamentoId);
    }

    public Optional<Pagamento> buscarPorAgendamento(String agendamentoId) {
        return pagamentoRepository.findByAgendamentoId(agendamentoId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> gerarRelatorio(YearMonth mesAno) {
        LocalDateTime inicio = mesAno.atDay(1).atStartOfDay();
        LocalDateTime fim = mesAno.atEndOfMonth().atTime(23, 59, 59);

        Map<String, Object> relatorio = new LinkedHashMap<>();

        BigDecimal total = pagamentoRepository.totalPorPeriodo(inicio, fim);
        relatorio.put("total", total);
        relatorio.put("mesAno", mesAno);

        List<Pagamento> pagamentos = pagamentoRepository.findByPeriodo(inicio, fim);
        relatorio.put("pagamentos", pagamentos);
        relatorio.put("quantidadeAtendimentos", pagamentos.size());

        Map<String, BigDecimal> porForma = new LinkedHashMap<>();
        for (Object[] row : pagamentoRepository.totalPorFormaPagamento(inicio, fim)) {
            porForma.put(row[0].toString(), (BigDecimal) row[1]);
        }
        relatorio.put("porFormaPagamento", porForma);

        List<Object[]> servicos = pagamentoRepository.servicosMaisRealizados(inicio, fim);
        relatorio.put("servicosMaisRealizados", servicos);

        List<Object[]> clientes = pagamentoRepository.clientesQueMaisGastaram(inicio, fim);
        relatorio.put("clientesDestaque", clientes);

        return relatorio;
    }
}
