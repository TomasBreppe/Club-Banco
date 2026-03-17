package com.example.demo.dto.ingresos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngresoDashboardItemDto {
    private String origen; // CUOTA o MANUAL
    private Long id;
    private LocalDateTime fecha;
    private String socioNombreCompleto;
    private String disciplinaNombre;
    private String categoria;
    private String concepto;
    private String periodo;
    private String medio;
    private BigDecimal monto;
    private String descripcion;
}