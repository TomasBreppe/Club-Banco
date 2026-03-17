package com.example.demo.dto.gasto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardGastosResponseDto {
    private DashboardGastosResumenDto resumen;
    private List<GastoDto> gastos;
}