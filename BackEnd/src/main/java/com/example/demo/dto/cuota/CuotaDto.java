package com.example.demo.dto.cuota;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CuotaDto {
    private Long id;
    private Long disciplinaId;
    private BigDecimal montoTotal;
    private LocalDate vigenteDesde;
    private Boolean activa;
}
