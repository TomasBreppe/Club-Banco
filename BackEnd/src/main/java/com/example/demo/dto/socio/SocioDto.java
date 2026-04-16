package com.example.demo.dto.socio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocioDto {
    private Long id;
    private String dni;
    private String nombre;
    private String apellido;
    private String genero;
    private String telefono;
    private String celular;
    private Long disciplinaId;
    private String disciplinaNombre;
    private Long arancelDisciplinaId;
    private String categoriaArancel;
    private LocalDate vigenciaHasta;
    private String estadoPago;
    private Boolean activo;
    private Boolean tieneBeca;
    private BigDecimal porcentajeBecaSocial;
    private BigDecimal porcentajeBecaDeportiva;
    private BigDecimal porcentajeBecaPreparacionFisica;
    private String observacionBeca;
}