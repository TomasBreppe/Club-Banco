package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cambio_disciplina")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CambioDisciplinaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "socio_id")
    private SocioEntity socio;

    @ManyToOne(optional = false) @JoinColumn(name = "origen_id")
    private DisciplinaEntity origen;

    @ManyToOne(optional = false) @JoinColumn(name = "destino_id")
    private DisciplinaEntity destino;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(length = 200)
    private String motivo;
}
