package com.example.demo.dto.ingresos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngresoManualDto {
    private Long id;
    private LocalDate fecha;
    private String categoria;
    private String medioPago;
    private BigDecimal monto;
    private String descripcion;
}