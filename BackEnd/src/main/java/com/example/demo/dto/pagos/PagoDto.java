package com.example.demo.dto.pagos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoDto {

    private Long id;
    private String concepto;
    private String periodo;

    private Long disciplinaId;
    private String disciplinaNombre;
    private String categoria;

    private BigDecimal montoTotal;
    private BigDecimal montoSocial;
    private BigDecimal montoDisciplina;
    private BigDecimal montoPreparacionFisica;

    private String medio;
    private String observacion;
    private LocalDateTime fechaPago;

    private Boolean anulado;
    private LocalDateTime fechaAnulacion;
    private String motivoAnulacion;
    
    private String mpPaymentId;
    private String mpStatus;
}