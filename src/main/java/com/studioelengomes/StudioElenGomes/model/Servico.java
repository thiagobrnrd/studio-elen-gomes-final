package com.studioelengomes.StudioElenGomes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "servicos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull(message = "Duração é obrigatória")
    @Column(name = "duracao_min", nullable = false)
    private Integer duracaoMin;

    @NotNull(message = "Preço é obrigatório")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = true;

    @Builder.Default
    @Column(name = "requer_retorno", nullable = false)
    private Boolean requerRetorno = false;

    /** Aparece na página pública de agendamento. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean online = true;

    /** Mostra "a partir de" antes do preço (preço varia conforme o cabelo). */
    @Builder.Default
    @Column(name = "preco_a_partir", nullable = false)
    private Boolean precoAPartir = true;

    @Column(name = "dias_para_retorno")
    private Integer diasParaRetorno;
}