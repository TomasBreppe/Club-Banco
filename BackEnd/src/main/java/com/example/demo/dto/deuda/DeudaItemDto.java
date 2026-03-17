package com.example.demo.dto.deuda;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeudaItemDto {
    private String periodo;         // YYYY-MM
    private BigDecimal monto;       // monto de ese mes
    private boolean pagado;         // para mostrar historial también si querés
}
