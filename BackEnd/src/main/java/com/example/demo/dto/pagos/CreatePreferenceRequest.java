package com.example.demo.dto.pagos;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreatePreferenceRequest {
    @NotNull
    private Long socioId;

    // "CUOTA_MENSUAL" o "INSCRIPCION"
    @NotNull
    private String concepto;
}
