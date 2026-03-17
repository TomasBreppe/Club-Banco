package com.example.demo.dto.disciplina;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisciplinaCreateDto {
    @NotBlank
    private String nombre;
}
