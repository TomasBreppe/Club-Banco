package com.example.demo.dto.deuda;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeudaResponseDto {
    private Long socioId;
    private String dni;
    private String nombreCompleto;
    private String disciplina;
    private LocalDate vigenciaHasta;

    private BigDecimal montoMensual;     // cuota de la disciplina (actual)
    private int mesesAdeudados;
    private BigDecimal totalAdeudado;

    private List<DeudaItemDto> items;    // detalle por mes
}
