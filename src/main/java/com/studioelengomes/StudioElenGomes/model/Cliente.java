package com.studioelengomes.StudioElenGomes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "Telefone é obrigatório")
    @Column(nullable = false)
    private String telefone;

    @Email(message = "E-mail inválido")
    private String email;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    /** LGPD: quando a cliente aceitou a política de privacidade (agendamento online). */
    @Column(name = "privacidade_aceita_em")
    private LocalDateTime privacidadeAceitaEm;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}