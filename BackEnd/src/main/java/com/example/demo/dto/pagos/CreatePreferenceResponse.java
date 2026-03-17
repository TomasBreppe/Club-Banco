package com.example.demo.dto.pagos;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreatePreferenceResponse {
    private String initPoint;   // link checkout
    private String preferenceId;
}