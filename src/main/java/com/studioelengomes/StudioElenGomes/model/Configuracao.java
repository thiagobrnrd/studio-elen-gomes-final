package com.studioelengomes.StudioElenGomes.model;

import jakarta.persistence.*;
import lombok.*;

/** Configurações do studio editáveis pelo painel (chave → valor). */
@Entity
@Table(name = "configuracoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configuracao {
    @Id
    @Column(length = 60)
    private String chave;

    @Column(columnDefinition = "TEXT")
    private String valor;
}
