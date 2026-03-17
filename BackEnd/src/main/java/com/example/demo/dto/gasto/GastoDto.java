package com.example.demo.dto.gasto;

import com.example.demo.entity.GastoCategoria;
import com.example.demo.entity.MedioPagoGasto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoDto {
    private Long id;
    private LocalDate fecha;
    private GastoCategoria categoria;
    private String concepto;
    private String descripcion;
    private BigDecimal monto;
    private MedioPagoGasto medioPago;
    private Boolean activo;
    private LocalDateTime createdAt;
}