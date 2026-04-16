package com.example.demo.dto.socio;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SocioBecaUpdateDto {
    private Boolean tieneBeca;
    private BigDecimal porcentajeBecaSocial;
    private BigDecimal porcentajeBecaDeportiva;
    private BigDecimal porcentajeBecaPreparacionFisica;
    private String observacionBeca;
}