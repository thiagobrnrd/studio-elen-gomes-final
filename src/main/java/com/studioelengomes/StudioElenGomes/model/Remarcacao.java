package com.studioelengomes.StudioElenGomes.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "remarcacoes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Remarcacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private Agendamento agendamento;
    @Column(name = "data_hora_anterior", nullable = false)
    private LocalDateTime dataHoraAnterior;
    @Column(name = "nova_data_hora", nullable = false)
    private LocalDateTime novaDataHora;
    private String motivo;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @PrePersist void prePersist(){criadoEm=LocalDateTime.now();}
}
