package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "socio_disciplina",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_socio_disciplina_activa",
            columnNames = {"socio_id", "disciplina_id", "activo"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocioDisciplinaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_id", nullable = false)
    private SocioEntity socio;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "disciplina_id", nullable = false)
    private DisciplinaEntity disciplina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arancel_disciplina_id")
    private ArancelDisciplinaEntity arancelDisciplina;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Column(name = "inscripcion_pagada", nullable = false)
    private Boolean inscripcionPagada = false;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (fechaAlta == null) fechaAlta = LocalDateTime.now();
        if (activo == null) activo = true;
        if (inscripcionPagada == null) inscripcionPagada = false;
    }
}