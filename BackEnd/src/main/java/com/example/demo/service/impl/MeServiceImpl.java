package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.me.MisSociosDto;
import com.example.demo.entity.SocioEntity;
import com.example.demo.entity.UsuarioEntity;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.MeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeServiceImpl implements MeService {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    @Override
    public BaseResponse<List<MisSociosDto>> misSocios(String email) {

        UsuarioEntity u = usuarioRepository.findByEmailWithSocios(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<MisSociosDto> list = u.getSocios().stream()
                .map(this::mapSocio)
                .toList();

        return BaseResponse.ok("OK", list);
    }

    private MisSociosDto mapSocio(SocioEntity s) {
        boolean alDia = s.getVigenciaHasta() != null && !s.getVigenciaHasta().isBefore(LocalDate.now());

        return MisSociosDto.builder()
                .id(s.getId())
                .dni(s.getDni())
                .nombre(s.getNombre())
                .apellido(s.getApellido())
                .disciplina(s.getDisciplina().getNombre())
                .vigenciaHasta(s.getVigenciaHasta())
                .estadoPago(alDia ? "AL_DIA" : "DEBE")
                .build();
    }
}
