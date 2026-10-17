package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.model.Agendamento;
import com.studioelengomes.StudioElenGomes.model.Cliente;
import com.studioelengomes.StudioElenGomes.repository.AgendamentoRepository;
import com.studioelengomes.StudioElenGomes.repository.ClienteRepository;
import com.studioelengomes.StudioElenGomes.service.AgendamentoService;
import com.studioelengomes.StudioElenGomes.service.ServicoService;
import com.studioelengomes.StudioElenGomes.util.MensagemLembrete;
import org.springframework.transaction.annotation.Transactional;
import com.studioelengomes.StudioElenGomes.util.TelefoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/relacionamento")
@RequiredArgsConstructor
public class RelacionamentoApiController {
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AgendamentoService agendamentoService;
    private final ServicoService servicoService;

    @Value("${studio.horario.abertura:09:00}")
    private String abertura;

    @Value("${studio.horario.fechamento:19:00}")
    private String fechamento;

    @Value("${studio.nome:Studio Elen Gomes}")
    private String studioNome;

    public record Birthday(String clienteId, String clienteNome, String telefone, LocalDate dataNascimento,
                           int dias, String link, String mensagem) {}
    public record Confirmacao(String agendamentoId, String clienteId, String clienteNome, String telefone,
                              String servicoNome, LocalDateTime dataHora, String link) {}
    public record Suggestion(String tipo, String titulo, String descricao, String clienteId, String clienteNome,
                             String link) {}
    public record Resumo(List<Birthday> aniversarios, List<Confirmacao> confirmacoes, List<Suggestion> sugestoes) {}
    public record Slot(LocalDateTime dataHora, String hora) {}

    @GetMapping("/resumo")
    @Transactional(readOnly = true)
    public Resumo resumo() {
        LocalDate hoje = LocalDate.now(ZONA);

        List<Birthday> aniversarios = clienteRepository.findAll().stream()
                .filter(c -> c.getDataNascimento() != null)
                .map(c -> birthday(c, hoje))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(Birthday::dias))
                .toList();

        LocalDateTime agora = LocalDateTime.now(ZONA);
        LocalDateTime fim = hoje.plusDays(1).atTime(23, 59, 59);
        List<Confirmacao> confirmacoes = agendamentoRepository
                .findByDataHoraBetweenOrderByDataHoraAsc(agora, fim).stream()
                .filter(a -> a.getStatus() == Agendamento.Status.AGENDADO)
                .map(this::confirmacao)
                .toList();

        List<Suggestion> sugestoes = new ArrayList<>();
        for (Birthday b : aniversarios.stream().limit(3).toList()) {
            sugestoes.add(new Suggestion("ANIVERSARIO", "Prepare uma promoção de aniversário",
                    b.clienteNome() + " faz aniversário " + (b.dias() == 0 ? "hoje" : "em " + b.dias() + " dia(s)"),
                    b.clienteId(), b.clienteNome(), b.link()));
        }

        LocalDateTime corte = agora.minusDays(60);
        clienteRepository.findAll().stream()
                .filter(c -> c.getTelefone() != null && !c.getTelefone().isBlank())
                .filter(c -> {
                    var historico = agendamentoRepository.findByClienteIdOrderByDataHoraDesc(c.getId()).stream()
                            .filter(a -> a.getStatus() != Agendamento.Status.CANCELADO).toList();
                    return !historico.isEmpty() && historico.stream().noneMatch(a -> a.getDataHora().isAfter(corte));
                })
                .limit(3)
                .forEach(c -> {
                    String msg = "Oi, " + MensagemLembrete.primeiroNome(c.getNome()) + "! 💛 Sentimos sua falta no " + studioNome + ". Que tal agendarmos seu próximo horário? ✨";
                    String link = TelefoneUtil.paraWhatsApp(c.getTelefone())
                            .map(t -> MensagemLembrete.linkWhatsApp(t, msg)).orElse(null);
                    sugestoes.add(new Suggestion("REATIVACAO", "Cliente para reativar",
                            c.getNome() + " está há mais de 60 dias sem atendimento.", c.getId(), c.getNome(), link));
                });

        return new Resumo(aniversarios, confirmacoes, sugestoes);
    }

    /**
     * Horários livres de um dia, de 30 em 30 minutos, dentro do expediente.
     * Use agendamentoId (remarcação: ignora o próprio atendimento) ou servicoId (novo agendamento).
     */
    @GetMapping("/horarios-disponiveis")
    @Transactional(readOnly = true)
    public List<Slot> horariosDisponiveis(@RequestParam(required = false) String agendamentoId,
                                          @RequestParam(required = false) String servicoId,
                                          @RequestParam String data) {
        int duracao;
        if (agendamentoId != null && !agendamentoId.isBlank()) {
            duracao = agendamentoService.buscarPorId(agendamentoId).getServico().getDuracaoMin();
        } else if (servicoId != null && !servicoId.isBlank()) {
            duracao = servicoService.buscarPorId(servicoId).getDuracaoMin();
        } else {
            throw new IllegalArgumentException("Informe o serviço.");
        }
        LocalDate dia = LocalDate.parse(data);
        LocalTime inicio = LocalTime.parse(abertura);
        LocalTime fim = LocalTime.parse(fechamento);
        LocalDateTime agora = LocalDateTime.now(ZONA);

        List<Slot> slots = new ArrayList<>();
        for (LocalTime hora = inicio; !hora.plusMinutes(duracao).isAfter(fim); hora = hora.plusMinutes(30)) {
            LocalDateTime candidato = LocalDateTime.of(dia, hora);
            if (candidato.isBefore(agora)) continue;
            if (!agendamentoService.temConflito(candidato, duracao, agendamentoId)) {
                slots.add(new Slot(candidato, hora.toString()));
            }
            if (hora.plusMinutes(30).isBefore(hora)) break; // virou o dia
        }
        return slots;
    }

    private Birthday birthday(Cliente c, LocalDate hoje) {
        LocalDate n = c.getDataNascimento();
        LocalDate proximo;
        try {
            proximo = n.withYear(hoje.getYear());
        } catch (java.time.DateTimeException ex) {
            proximo = LocalDate.of(hoje.getYear(), 2, 28);
        }
        if (proximo.isBefore(hoje)) {
            int ano = hoje.getYear() + 1;
            try {
                proximo = n.withYear(ano);
            } catch (java.time.DateTimeException ex) {
                proximo = LocalDate.of(ano, 2, 28);
            }
        }
        long dias = ChronoUnit.DAYS.between(hoje, proximo);
        if (dias > 7) return null;
        String nome = MensagemLembrete.primeiroNome(c.getNome());
        String msg = dias == 0
                ? "Oi, " + nome + "! 🎂💛 Feliz aniversário! O Studio Elen Gomes preparou uma condição especial para você. Que tal aproveitar seu dia para se cuidar? ✨"
                : "Oi, " + nome + "! 💛 Seu aniversário está chegando e queremos comemorar com você. Preparamos uma condição especial no Studio Elen Gomes. 🎂✨";
        String link = TelefoneUtil.paraWhatsApp(c.getTelefone()).map(t -> MensagemLembrete.linkWhatsApp(t, msg)).orElse(null);
        return new Birthday(c.getId(), c.getNome(), c.getTelefone(), n, (int) dias, link, msg);
    }

    private Confirmacao confirmacao(Agendamento a) {
        String msg = "Olá, " + MensagemLembrete.primeiroNome(a.getCliente().getNome()) + "! 💛 Seu atendimento no " + studioNome + " está marcado para "
                + a.getDataHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"))
                + ", para " + a.getServico().getNome() + ". Podemos confirmar seu horário? ✨";
        String link = TelefoneUtil.paraWhatsApp(a.getCliente().getTelefone())
                .map(t -> MensagemLembrete.linkWhatsApp(t, msg)).orElse(null);
        return new Confirmacao(a.getId(), a.getCliente().getId(), a.getCliente().getNome(), a.getCliente().getTelefone(),
                a.getServico().getNome(), a.getDataHora(), link);
    }
}
