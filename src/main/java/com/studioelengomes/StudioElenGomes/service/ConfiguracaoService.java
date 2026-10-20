package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.model.Configuracao;
import com.studioelengomes.StudioElenGomes.repository.ConfiguracaoRepository;
import com.studioelengomes.StudioElenGomes.util.TelefoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lê e grava as configurações do studio (tabela "configuracoes").
 * Quando uma chave não existe no banco, usa o valor padrão abaixo.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracaoService {

    private final ConfiguracaoRepository repository;

    @Value("${studio.whatsapp:}")
    private String whatsappPadrao;

    @Value("${studio.horario.abertura:09:00}")
    private String aberturaPadrao;

    @Value("${studio.horario.fechamento:19:00}")
    private String fechamentoPadrao;

    private volatile StudioConfig cache;

    /** Chaves aceitas e seus valores padrão. */
    private Map<String, String> padroes() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("online_ativo", "true");
        m.put("aprovacao_automatica", "false");
        m.put("dias_funcionamento", "2,3,4,5,6"); // 1 = segunda ... 7 = domingo
        m.put("abertura", aberturaPadrao);
        m.put("fechamento", fechamentoPadrao);
        m.put("almoco_inicio", "");
        m.put("almoco_fim", "");
        m.put("intervalo_minutos", "30");
        m.put("antecedencia_min_horas", "2");
        m.put("dias_maximos", "30");
        m.put("endereco", "");
        m.put("instagram", "");
        m.put("whatsapp", whatsappPadrao == null ? "" : whatsappPadrao);
        m.put("politica", "Cancelamentos ou remarcações devem ser avisados com pelo menos 24 horas de antecedência.");
        m.put("mensagem", "Escolha o serviço, o dia e o horário. Seu pedido será confirmado pelo WhatsApp.");
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, String> valores() {
        Map<String, String> m = padroes();
        repository.findAll().forEach(c -> {
            if (m.containsKey(c.getChave())) m.put(c.getChave(), c.getValor() == null ? "" : c.getValor());
        });
        return m;
    }

    public StudioConfig atual() {
        StudioConfig c = cache;
        if (c == null) {
            c = converter(valores());
            cache = c;
        }
        return c;
    }

    @Transactional
    public Map<String, String> salvar(Map<String, String> novos) {
        Map<String, String> atuais = valores();
        Map<String, String> mesclado = new LinkedHashMap<>(atuais);
        novos.forEach((k, v) -> {
            if (atuais.containsKey(k)) mesclado.put(k, v == null ? "" : v.trim());
        });
        converter(mesclado); // valida antes de gravar (lança IllegalArgumentException)
        mesclado.forEach((k, v) -> {
            if (novos.containsKey(k)) repository.save(new Configuracao(k, v));
        });
        cache = null;
        return valores();
    }

    private StudioConfig converter(Map<String, String> m) {
        LocalTime abertura = hora(m.get("abertura"), "Horário de abertura");
        LocalTime fechamento = hora(m.get("fechamento"), "Horário de fechamento");
        if (abertura == null || fechamento == null || !fechamento.isAfter(abertura)) {
            throw new IllegalArgumentException("O horário de fechamento precisa ser depois da abertura.");
        }
        LocalTime almocoIni = hora(m.get("almoco_inicio"), "Início do intervalo");
        LocalTime almocoFim = hora(m.get("almoco_fim"), "Fim do intervalo");
        if ((almocoIni == null) != (almocoFim == null)) {
            throw new IllegalArgumentException("Preencha o início e o fim do intervalo, ou deixe os dois em branco.");
        }
        if (almocoIni != null && !almocoFim.isAfter(almocoIni)) {
            throw new IllegalArgumentException("O fim do intervalo precisa ser depois do início.");
        }
        Set<DayOfWeek> dias = Arrays.stream(m.getOrDefault("dias_funcionamento", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> {
                    try { return DayOfWeek.of(Integer.parseInt(s)); }
                    catch (Exception e) { throw new IllegalArgumentException("Dia de funcionamento inválido: " + s); }
                })
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
        int intervalo = inteiro(m.get("intervalo_minutos"), 5, 120, "Intervalo entre horários");
        int antecedencia = inteiro(m.get("antecedencia_min_horas"), 0, 168, "Antecedência mínima");
        int diasMax = inteiro(m.get("dias_maximos"), 1, 180, "Dias à frente");
        String whatsapp = TelefoneUtil.paraWhatsApp(m.get("whatsapp")).orElse(m.get("whatsapp"));
        return new StudioConfig(
                Boolean.parseBoolean(m.get("online_ativo")),
                Boolean.parseBoolean(m.get("aprovacao_automatica")),
                dias, abertura, fechamento, almocoIni, almocoFim,
                intervalo, antecedencia, diasMax,
                m.get("endereco"), m.get("instagram"), whatsapp, m.get("politica"), m.get("mensagem"));
    }

    private static LocalTime hora(String v, String campo) {
        if (v == null || v.isBlank()) return null;
        try {
            return LocalTime.parse(v.trim().length() == 4 ? "0" + v.trim() : v.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(campo + " inválido. Use o formato 09:00.");
        }
    }

    private static int inteiro(String v, int min, int max, String campo) {
        try {
            int n = Integer.parseInt(v.trim());
            if (n < min || n > max) throw new NumberFormatException();
            return n;
        } catch (Exception e) {
            throw new IllegalArgumentException(campo + ": informe um número entre " + min + " e " + max + ".");
        }
    }
}
