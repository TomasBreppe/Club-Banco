package com.example.demo.dto.pagos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoDashboardDto {
    private Long id;
    private Long socioId;
    private String socioNombreCompleto;
    private String disciplinaNombre;
    private String concepto;
    private String periodo;
    private BigDecimal montoTotal;
    private String medio;
    private LocalDateTime fechaPago;
    private String mpPaymentId;
    private String mpStatus;
}