package com.example.demo.dto.arancel;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArancelDisciplinaDto {

    private Long id;
    private Long disciplinaId;
    private String disciplinaNombre;
    private String categoria;

    private BigDecimal montoSocial;
    private BigDecimal montoDeportivo;
    private BigDecimal montoPreparacionFisica;
    private BigDecimal montoTotal;

    private LocalDate vigenteDesde;
    private Boolean activa;
}