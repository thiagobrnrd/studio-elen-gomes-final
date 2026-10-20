package com.studioelengomes.StudioElenGomes.api;

import com.studioelengomes.StudioElenGomes.service.ConfiguracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoApiController {
    private final ConfiguracaoService service;

    @GetMapping
    public Map<String, String> listar() {
        return service.valores();
    }

    @PutMapping
    public Map<String, String> salvar(@RequestBody Map<String, String> valores) {
        return service.salvar(valores);
    }
}
