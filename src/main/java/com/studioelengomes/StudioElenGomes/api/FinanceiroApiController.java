package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.model.Pagamento;
import com.studioelengomes.StudioElenGomes.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
public class FinanceiroApiController {
    private final PagamentoService service;

    public record PagamentoRequest(BigDecimal valor, Pagamento.FormaPagamento formaPagamento) {}
    public record PagamentoDto(String id, String clienteNome, String servicoNome, BigDecimal valor, String formaPagamento, java.time.LocalDateTime pagoEm) {}
    public record ServicoResumo(String nome, long quantidade, BigDecimal total) {}
    public record ClienteResumo(String nome, long quantidade, BigDecimal total) {}

    @GetMapping("/relatorio")
    public Map<String,Object> relatorio(@RequestParam String mes) {
        YearMonth mesAno = YearMonth.parse(mes);
        Map<String,Object> raw = service.gerarRelatorio(mesAno);
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("mesAno", mesAno.toString());
        out.put("total", raw.get("total"));
        out.put("quantidadeAtendimentos", raw.get("quantidadeAtendimentos"));

        List<Pagamento> pagamentos = (List<Pagamento>) raw.get("pagamentos");
        out.put("pagamentos", pagamentos.stream().map(p -> new PagamentoDto(
                p.getId(), p.getAgendamento().getCliente().getNome(), p.getAgendamento().getServico().getNome(),
                p.getValor(), p.getFormaPagamento().name(), p.getPagoEm())).toList());
        out.put("porFormaPagamento", raw.get("porFormaPagamento"));

        List<Object[]> servicos = (List<Object[]>) raw.get("servicosMaisRealizados");
        out.put("servicosMaisRealizados", servicos.stream()
                .map(r -> new ServicoResumo((String) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2])).toList());

        List<Object[]> clientes = (List<Object[]>) raw.get("clientesDestaque");
        out.put("clientesDestaque", clientes.stream()
                .map(r -> new ClienteResumo((String) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2])).toList());
        return out;
    }

    @PostMapping("/pagamentos/{agendamentoId}")
    public PagamentoDto registrar(@PathVariable String agendamentoId, @Valid @RequestBody PagamentoRequest request) {
        if (request.valor() == null || request.formaPagamento() == null) {
            throw new IllegalArgumentException("Valor e forma de pagamento são obrigatórios.");
        }
        if (service.jaFoiPago(agendamentoId)) {
            throw new IllegalStateException("Este agendamento já possui um pagamento registrado.");
        }
        Pagamento pagamento = Pagamento.builder().valor(request.valor()).formaPagamento(request.formaPagamento()).build();
        Pagamento salvo = service.registrar(agendamentoId, pagamento);
        return new PagamentoDto(salvo.getId(), salvo.getAgendamento().getCliente().getNome(), salvo.getAgendamento().getServico().getNome(), salvo.getValor(), salvo.getFormaPagamento().name(), salvo.getPagoEm());
    }
}
