package com.studioelengomes.StudioElenGomes.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fichas_capilares")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichaCapilar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo_cabelo")
    private String tipoCabelo;

    @Column(name = "historico_quimicos", columnDefinition = "TEXT")
    private String historicoQuimicos;

    @Column(columnDefinition = "TEXT")
    private String alergias;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
