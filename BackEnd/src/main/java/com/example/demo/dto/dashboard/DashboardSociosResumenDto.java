package com.example.demo.dto.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSociosResumenDto {
    private long totalSocios;
    private long activos;
    private long inactivos;
    private long alDia;
    private long debe;
}