package com.example.demo.dto.gasto;

import com.example.demo.entity.GastoCategoria;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoCreateDto {
    private LocalDate fecha;
    private GastoCategoria categoria;
    private String concepto;
    private String descripcion;
    private BigDecimal monto;
    private String medioPago;
}