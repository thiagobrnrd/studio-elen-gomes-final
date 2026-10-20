package com.studioelengomes.StudioElenGomes.service;

import com.studioelengomes.StudioElenGomes.model.*;
import com.studioelengomes.StudioElenGomes.repository.NotificacaoRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final JavaMailSender mailSender;
    private final NotificacaoRepository notificacaoRepository;

    @Value("${studio.nome}")
    private String studioNome;

    @Value("${studio.email.remetente}")
    private String emailRemetente;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    public void enviarLembrete24h(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();

        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            log.warn("Cliente {} sem e-mail cadastrado, lembrete ignorado.", cliente.getNome());
            registrarNotificacao(agendamento, cliente,
                    Notificacao.TipoNotificacao.LEMBRETE_24H,
                    Notificacao.StatusNotificacao.FALHA,
                    "E-mail não cadastrado");
            return;
        }

        try {
            String html = htmlLembrete24h(cliente.getNome(), agendamento.getServico().getNome(),
                    agendamento.getDataHora().format(FORMATTER));
            String assunto = "🗓️ Lembrete: seu agendamento é amanhã! – " + studioNome;

            enviarEmail(cliente.getEmail(), assunto, html);

            registrarNotificacao(agendamento, cliente,
                    Notificacao.TipoNotificacao.LEMBRETE_24H,
                    Notificacao.StatusNotificacao.ENVIADA,
                    "Lembrete 24h enviado com sucesso");

            log.info("Lembrete 24h enviado para {} ({})", cliente.getNome(), cliente.getEmail());

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Erro ao enviar lembrete 24h para {}: {}", cliente.getEmail(), e.getMessage());
            registrarNotificacao(agendamento, cliente,
                    Notificacao.TipoNotificacao.LEMBRETE_24H,
                    Notificacao.StatusNotificacao.FALHA,
                    e.getMessage());
        }
    }

    public void enviarLembreteRetorno(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();

        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            log.warn("Cliente {} sem e-mail, lembrete de retorno ignorado.", cliente.getNome());
            registrarNotificacao(agendamento, cliente,
                    Notificacao.TipoNotificacao.LEMBRETE_RETORNO,
                    Notificacao.StatusNotificacao.FALHA,
                    "E-mail não cadastrado");
            return;
        }

        try {
            String html = htmlLembreteRetorno(cliente.getNome(), agendamento.getServico().getNome(),
                    agendamento.getServico().getDiasParaRetorno());
            String assunto = "💛 Está na hora de renovar seu visual! – " + studioNome;

            enviarEmail(cliente.getEmail(), assunto, html);

            registrarNotificacao(agendamento, cliente,
                    Notificacao.TipoNotificacao.LEMBRETE_RETORNO,
                    Notificacao.StatusNotificacao.ENVIADA,
                    "Lembrete de retorno enviado");

            log.info("Lembrete retorno enviado para {}", cliente.getNome());

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Erro ao enviar lembrete retorno: {}", e.getMessage());
            registrarNotificacao(agendamento, cliente,
                    Notificacao.TipoNotificacao.LEMBRETE_RETORNO,
                    Notificacao.StatusNotificacao.FALHA,
                    e.getMessage());
        }
    }

    private String htmlLembrete24h(String nomeCliente, String nomeServico, String dataHora) {
        return """
            <!doctype html><html lang="pt-BR"><body style="margin:0;background:#080706;font-family:Arial,sans-serif;color:#f5eee4">
            <div style="max-width:620px;margin:30px auto;padding:34px;background:#11100e;border:1px solid #8a6a3d;border-radius:16px">
              <div style="text-align:center;color:#d8b06a;font-size:22px;font-weight:bold">%s</div>
              <h1 style="text-align:center;font-size:24px;font-weight:500">Seu horário está chegando</h1>
              <p>Olá, <strong>%s</strong>! Este é um lembrete do seu atendimento no <strong>%s</strong>.</p>
              <p style="font-size:18px;color:#e8c986"><strong>%s</strong><br>%s</p>
              <p>Se precisar remarcar, entre em contato com o studio o quanto antes.</p>
              <p style="text-align:center;color:#9f9387;margin-top:30px">Beleza • Autoestima • Você</p>
            </div></body></html>
            """.formatted(escapeHtml(studioNome), escapeHtml(nomeCliente), escapeHtml(studioNome),
                    escapeHtml(nomeServico), escapeHtml(dataHora));
    }

    private String htmlLembreteRetorno(String nomeCliente, String nomeServico, Integer diasRetorno) {
        String prazo = diasRetorno == null ? "" : "Seu serviço tem retorno recomendado em aproximadamente " + diasRetorno + " dias.";
        return """
            <!doctype html><html lang="pt-BR"><body style="margin:0;background:#080706;font-family:Arial,sans-serif;color:#f5eee4">
            <div style="max-width:620px;margin:30px auto;padding:34px;background:#11100e;border:1px solid #8a6a3d;border-radius:16px">
              <div style="text-align:center;color:#d8b06a;font-size:22px;font-weight:bold">%s</div>
              <h1 style="text-align:center;font-size:24px;font-weight:500">Está na hora de renovar seu visual</h1>
              <p>Olá, <strong>%s</strong>! 💛</p>
              <p>Seu último procedimento foi <strong>%s</strong>.</p>
              <p>%s</p>
              <p>Que tal agendar seu próximo horário?</p>
              <p style="text-align:center;color:#9f9387;margin-top:30px">Beleza • Autoestima • Você</p>
            </div></body></html>
            """.formatted(escapeHtml(studioNome), escapeHtml(nomeCliente), escapeHtml(nomeServico), escapeHtml(prazo));
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void enviarEmail(String destinatario, String assunto, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(emailRemetente, studioNome);
        helper.setTo(destinatario);
        helper.setSubject(assunto);
        helper.setText(htmlBody, true);

        mailSender.send(message);
    }

    private void registrarNotificacao(Agendamento agendamento, Cliente cliente,
            Notificacao.TipoNotificacao tipo, Notificacao.StatusNotificacao status,
            String mensagem) {

        Notificacao notificacao = Notificacao.builder()
                .agendamento(agendamento)
                .cliente(cliente)
                .tipo(tipo)
                .status(status)
                .canal("EMAIL")
                .mensagem(mensagem)
                .criadaEm(LocalDateTime.now())
                .enviadaEm(status == Notificacao.StatusNotificacao.ENVIADA
                        ? LocalDateTime.now()
                        : null)
                .build();

        notificacaoRepository.save(notificacao);
    }
}
