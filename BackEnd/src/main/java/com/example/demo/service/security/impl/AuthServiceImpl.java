package com.example.demo.service.security.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.auth.ChangePasswordRequest;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.entity.UsuarioEntity;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.security.AuthService;
import com.example.demo.service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public BaseResponse<LoginResponse> login(LoginRequest req) {
        UsuarioEntity u = usuarioRepository.findByEmailIgnoreCase(req.getEmail().trim())
                .orElse(null);

        if (u == null || !Boolean.TRUE.equals(u.getActivo())) {
            return BaseResponse.bad("Credenciales inválidas");
        }

        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            return BaseResponse.bad("Credenciales inválidas");
        }

        u.setLastLogin(LocalDateTime.now());
        usuarioRepository.save(u);

        String token = jwtService.generateToken(u.getEmail(), String.valueOf(u.getRol()));

        return BaseResponse.ok("Login OK", LoginResponse.builder()
                .token(token)
                .rol(String.valueOf(u.getRol()))
                .mustChangePassword(u.getMustChangePassword())
                .build());
    }

    @Override
    public BaseResponse<Void> changePassword(String email, ChangePasswordRequest req) {
        UsuarioEntity u = usuarioRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        if (u == null) return BaseResponse.bad("Usuario no encontrado");
        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPasswordHash())) {
            return BaseResponse.bad("Contraseña actual incorrecta");
        }

        // reglas mínimas (podés endurecer después)
        if (req.getNewPassword().length() < 8) {
            return BaseResponse.bad("La nueva contraseña debe tener al menos 8 caracteres");
        }

        u.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        u.setMustChangePassword(false);
        usuarioRepository.save(u);

        return BaseResponse.ok("Contraseña actualizada", null);
    }
}
