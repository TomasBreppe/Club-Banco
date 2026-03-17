package com.example.demo.dto.pagos;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoManualRequestDto {

    private Long socioId;
    private String concepto; // CUOTA_MENSUAL / INSCRIPCION
    private String periodo;  // YYYY-MM

    private Long disciplinaId;
    private Long arancelDisciplinaId;
    private String categoria;

    private BigDecimal montoTotal;
    private BigDecimal montoSocial;
    private BigDecimal montoDisciplina;
    private BigDecimal montoPreparacionFisica;

    private String medio;
    private String observacion;
}