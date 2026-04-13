package com.example.demo.dto.socio;

import com.example.demo.dto.deuda.DeudaResponseDto;
import lombok.*;

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
}