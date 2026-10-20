package com.studioelengomes.StudioElenGomes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agendamento {

    public enum Status {
        SOLICITADO, AGENDADO, CONFIRMADO, CONCLUIDO, CANCELADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @NotNull(message = "Data e hora são obrigatórias")
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.AGENDADO;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    /** STUDIO = marcado pela Elen no painel; ONLINE = pedido feito pela cliente no link /agendar. */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String origem = "STUDIO";

    /** Código secreto do link que a cliente usa para acompanhar/cancelar o pedido online. */
    @Column(length = 40, unique = true)
    private String token;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}