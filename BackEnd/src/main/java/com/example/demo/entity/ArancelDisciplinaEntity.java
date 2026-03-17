package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "arancel_disciplina")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArancelDisciplinaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "disciplina_id")
    private DisciplinaEntity disciplina;

    @Column(nullable = false, length = 80)
    private String categoria; // GENERAL, MINI/U13, INFANTILES, PRIMERA, etc

    @Column(name = "monto_social", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSocial = BigDecimal.ZERO;

    @Column(name = "monto_deportivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDeportivo = BigDecimal.ZERO;

    @Column(name = "monto_preparacion_fisica", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPreparacionFisica = BigDecimal.ZERO;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde = LocalDate.now();

    @Column(nullable = false)
    private Boolean activa = true;
}