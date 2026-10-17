package com.studioelengomes.StudioElenGomes.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
public record AgendamentoRequest(@NotBlank String clienteId, @NotBlank String servicoId, @NotNull LocalDateTime dataHora, String observacoes) {}
