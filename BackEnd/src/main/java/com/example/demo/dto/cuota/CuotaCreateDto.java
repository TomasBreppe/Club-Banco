package com.example.demo.dto.cuota;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CuotaCreateDto {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal montoTotal;

    // opcional, si no viene usamos hoy
    private LocalDate vigenteDesde;
}
