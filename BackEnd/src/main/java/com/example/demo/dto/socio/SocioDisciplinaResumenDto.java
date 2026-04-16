package com.example.demo.dto.socio;

import com.example.demo.dto.deuda.DeudaResponseDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocioDisciplinaResumenDto {
    private Long socioDisciplinaId;
    private Long disciplinaId;
    private String disciplinaNombre;
    private Long arancelDisciplinaId;
    private String categoriaArancel;
    private LocalDate vigenciaHasta;
    private Boolean inscripcionPagada;
    private DeudaResponseDto deuda;

    private Boolean tieneBeca;
    private BigDecimal porcentajeBecaSocial;
    private BigDecimal porcentajeBecaDeportiva;
    private BigDecimal porcentajeBecaPreparacionFisica;
    private String observacionBeca;
}