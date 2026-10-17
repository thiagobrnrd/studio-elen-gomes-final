package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.dto.LembreteWhatsApp;
import com.studioelengomes.StudioElenGomes.model.Notificacao.TipoNotificacao;
import com.studioelengomes.StudioElenGomes.service.LembreteWhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Central de lembretes por WhatsApp (link wa.me): o sistema monta a mensagem e
 * a Elen envia pelo próprio WhatsApp. Ao clicar em "Enviar", o lembrete é
 * marcado como enviado para não aparecer de novo.
 */
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppApiController {
    private final LembreteWhatsAppService service;

    public record Central(List<LembreteWhatsApp> horarios, List<LembreteWhatsApp> retornos, List<LembreteWhatsApp> enviadosHoje) {}

    @GetMapping("/central")
    public Central central() {
        return new Central(service.listarHorarios(), service.listarRetornos(), service.listarEnviadosHoje());
    }

    @GetMapping("/lembretes")
    public List<LembreteWhatsApp> listar() {
        return service.listarHorarios();
    }

    @GetMapping("/retornos")
    public List<LembreteWhatsApp> retornos() {
        return service.listarRetornos();
    }

    @GetMapping("/enviados")
    public List<LembreteWhatsApp> enviados() {
        return service.listarEnviadosHoje();
    }

    @PostMapping("/marcar")
    public void marcar(@RequestParam String agendamentoId, @RequestParam TipoNotificacao tipo) {
        service.marcarEnviado(agendamentoId, tipo);
    }

    @DeleteMapping("/enviados/{notificacaoId}")
    public void desfazer(@PathVariable Long notificacaoId) {
        service.desfazer(notificacaoId);
    }
}
