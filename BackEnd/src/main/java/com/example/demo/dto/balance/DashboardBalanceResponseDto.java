package com.example.demo.dto.balance;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardBalanceResponseDto {
    private DashboardBalanceResumenDto resumen;
}