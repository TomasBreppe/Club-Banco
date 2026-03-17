package com.example.demo.config;

import com.example.demo.entity.RolUsuario;
import com.example.demo.entity.UsuarioEntity;
import com.example.demo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminBootstrap {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initAdmin() {
        return args -> {
            String email = System.getenv().getOrDefault("APP_ADMIN_EMAIL", "admin@club.com");
            String pass  = System.getenv().getOrDefault("APP_ADMIN_PASS", "Admin1234!");

            usuarioRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
                UsuarioEntity u = UsuarioEntity.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(pass))
                        .rol(RolUsuario.valueOf("ADMIN"))
                        .activo(true)
                        .mustChangePassword(true) // así el admin la cambia al entrar
                        .build();
                return usuarioRepository.save(u);
            });
        };
    }
}
