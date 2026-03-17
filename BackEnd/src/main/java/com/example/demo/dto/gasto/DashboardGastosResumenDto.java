package com.example.demo.dto.gasto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardGastosResumenDto {
    private BigDecimal totalMes;
    private Long cantidadGastosMes;
    private String categoriaMayorGasto;
}