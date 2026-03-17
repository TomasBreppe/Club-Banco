package com.example.demo.dto.auth;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private String token;
    private String rol;
    private Boolean mustChangePassword;
}
