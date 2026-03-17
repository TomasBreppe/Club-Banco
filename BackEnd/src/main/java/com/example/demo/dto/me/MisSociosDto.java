package com.example.demo.dto.me;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MisSociosDto {
    private Long id;
    private String dni;
    private String nombre;
    private String apellido;
    private String disciplina;
    private LocalDate vigenciaHasta;
    private String estadoPago; // AL_DIA / DEBE
}
