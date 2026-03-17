package com.example.demo.dto.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSociosDto {
    private long totalSocios;
    private long alDia;
    private long debe;
}
