package com.studioelengomes.StudioElenGomes.api.dto;
import java.time.LocalDate;
/** whatsapp = número no formato internacional (5511999999999) ou null se o telefone estiver incompleto. */
public record ClienteDto(String id, String nome, String telefone, String email, LocalDate dataNascimento, long totalAtendimentos, String whatsapp) {}
