package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingreso_manual")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngresoManualEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoriaIngresoManual categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 30)
    private MedioIngresoManual medioPago;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}