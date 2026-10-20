package com.studioelengomes.StudioElenGomes.model;

import jakarta.persistence.*;
import lombok.*;

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
