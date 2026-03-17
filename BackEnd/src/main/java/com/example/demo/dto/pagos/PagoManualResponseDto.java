package com.example.demo.dto.pagos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoManualResponseDto {

    private Long pagoId;
    private Long socioId;
    private Long disciplinaId;
    private String disciplinaNombre;
    private Long arancelDisciplinaId;
    private String categoria;
    private String concepto;
    private String periodo;
    private BigDecimal montoTotal;
    private BigDecimal montoSocial;
    private BigDecimal montoDisciplina;
    private BigDecimal montoPreparacionFisica;
    private String medio;
    private String observacion;
    private LocalDateTime fechaPago;
}