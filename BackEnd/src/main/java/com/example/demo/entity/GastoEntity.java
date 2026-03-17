package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gasto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GastoCategoria categoria;

    @Column(nullable = false, length = 120)
    private String concepto;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", length = 50)
    private MedioPagoGasto medioPago;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (fecha == null) fecha = LocalDate.now();
        if (activo == null) activo = true;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}