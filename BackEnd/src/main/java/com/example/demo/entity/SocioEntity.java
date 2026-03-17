package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "socio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", nullable = false)
    private Genero genero;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 30)
    private String celular;

    @ManyToOne(optional = false)
    @JoinColumn(name = "disciplina_id")
    private DisciplinaEntity disciplina;

    @ManyToOne
    @JoinColumn(name = "arancel_disciplina_id")
    private ArancelDisciplinaEntity arancelDisciplina;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Column(name = "inscripcion_pagada", nullable = false)
    private Boolean inscripcionPagada = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (activo == null) activo = true;
        if (inscripcionPagada == null) inscripcionPagada = false;
    }
}