package com.example.demo.dto.disciplina;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisciplinaDto {
    private Long id;
    private String nombre;
    private Boolean activa;
}
