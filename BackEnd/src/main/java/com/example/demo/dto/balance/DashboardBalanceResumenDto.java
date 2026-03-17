package com.example.demo.dto.balance;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardBalanceResumenDto {
    private BigDecimal ingresosMes;
    private BigDecimal gastosMes;
    private BigDecimal netoMes;
}