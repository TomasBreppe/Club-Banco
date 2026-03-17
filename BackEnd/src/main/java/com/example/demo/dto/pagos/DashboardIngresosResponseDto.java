package com.example.demo.dto.pagos;

import com.example.demo.dto.ingresos.IngresoDashboardItemDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardIngresosResponseDto {
    private DashboardIngresosResumenDto resumen;
    private List<IngresoDashboardItemDto> ingresos;
}