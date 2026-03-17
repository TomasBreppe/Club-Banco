package com.example.demo.dto.arancel;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArancelCreateRequestDto {

    private Long disciplinaId;
    private String categoria;
    private BigDecimal montoSocial;
    private BigDecimal montoDeportivo;
    private BigDecimal montoPreparacionFisica;
    private LocalDate vigenteDesde;
}