package com.example.demo.dto.socio;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocioCambiarDisciplinaDto {
    private Long disciplinaId;
    private Long arancelDisciplinaId;
    private Boolean inscripcionPagada;
    private Integer mesInicioPago;
}