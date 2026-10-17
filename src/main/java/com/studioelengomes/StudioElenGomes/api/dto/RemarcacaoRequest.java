package com.studioelengomes.StudioElenGomes.api.dto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
public record RemarcacaoRequest(@NotNull LocalDateTime novaDataHora, String motivo) {}
