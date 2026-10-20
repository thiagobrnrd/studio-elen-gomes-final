package com.studioelengomes.StudioElenGomes.api.dto;
import java.time.LocalDate;
public record ClienteDto(String id, String nome, String telefone, String email, LocalDate dataNascimento, long totalAtendimentos, String whatsapp) {}
