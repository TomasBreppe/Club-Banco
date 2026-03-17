package com.example.demo.dto.dashboard;

import com.example.demo.dto.socio.SocioDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSociosResponseDto {
    private DashboardSociosResumenDto resumen;
    private List<SocioDto> socios;
}