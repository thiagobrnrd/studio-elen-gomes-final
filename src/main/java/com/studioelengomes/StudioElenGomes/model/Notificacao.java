package com.studioelengomes.StudioElenGomes.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;

    @Enumerated(EnumType.STRING)
    private StatusNotificacao status;

    private String canal;
    @Column(columnDefinition = "TEXT")
    private String mensagem;
    private LocalDateTime enviadaEm;
    private LocalDateTime criadaEm;

    public enum TipoNotificacao {
        LEMBRETE_24H, LEMBRETE_RETORNO, CONFIRMACAO
    }

    public enum StatusNotificacao {
        PENDENTE, ENVIADA, FALHA
    }
}
