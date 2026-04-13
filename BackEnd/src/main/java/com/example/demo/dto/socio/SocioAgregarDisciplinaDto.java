package com.example.demo.dto.socio;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocioAgregarDisciplinaDto {
    private Long disciplinaId;
    private Long arancelDisciplinaId;
    private Boolean inscripcionPagada;
}