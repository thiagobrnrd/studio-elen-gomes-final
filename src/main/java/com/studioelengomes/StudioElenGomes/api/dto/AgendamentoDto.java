package com.studioelengomes.StudioElenGomes.api.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record AgendamentoDto(String id, String clienteId, String clienteNome, String telefone, String whatsapp,
                             String servicoId, String servicoNome, Integer duracaoMin, BigDecimal valor,
                             LocalDateTime dataHora, String status, String observacoes, boolean pago,
                             String origem, String motivoCancelamento, LocalDateTime criadoEm, String token) {}
