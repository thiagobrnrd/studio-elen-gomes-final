package com.studioelengomes.StudioElenGomes.api.dto;
import java.math.BigDecimal;
public record ServicoDto(String id, String nome, String descricao, Integer duracaoMin, BigDecimal preco, Boolean ativo, Boolean requerRetorno, Integer diasParaRetorno) {}
