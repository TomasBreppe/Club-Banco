package com.example.demo.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardGeneralDto {
    private Long sociosActivos;
    private BigDecimal ingresosMes;
    private BigDecimal gastosMes;
    private BigDecimal balanceMes;
}