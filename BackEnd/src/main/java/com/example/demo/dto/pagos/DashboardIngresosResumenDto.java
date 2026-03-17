package com.example.demo.dto.pagos;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardIngresosResumenDto {
    private BigDecimal totalMes;
    private Long cantidadPagosMes;
    private String medioMasUsado;
}