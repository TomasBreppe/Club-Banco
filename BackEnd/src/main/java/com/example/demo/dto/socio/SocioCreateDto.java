package com.example.demo.dto.socio;

import lombok.*;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocioCreateDto {

    @NotBlank
    @Size(max = 20)
    private String dni;

    @NotBlank
    @Size(max = 80)
    private String nombre;

    @NotBlank
    @Size(max = 80)
    private String apellido;

    @NotBlank
    private String genero; // MASCULINO / FEMENINO / OTRO

    @Size(max = 30)
    private String telefono; // opcional

    @NotBlank
    @Size(max = 30)
    private String celular;

    @NotNull
    private Long disciplinaId;

    @NotNull
    private Long arancelDisciplinaId;

    private Boolean inscripcionPagada;

    private Boolean tieneBeca;
    private BigDecimal porcentajeBecaSocial;
    private BigDecimal porcentajeBecaDeportiva;
    private BigDecimal porcentajeBecaPreparacionFisica;
    private String observacionBeca; 
}