package com.example.demo.dto.usuario;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VincularSocioDto {
    @NotNull
    private Long socioId;
}
