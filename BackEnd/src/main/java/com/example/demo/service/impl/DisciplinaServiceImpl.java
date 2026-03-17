package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.disciplina.*;
import com.example.demo.entity.DisciplinaEntity;
import com.example.demo.repository.DisciplinaRepository;
import com.example.demo.service.DisciplinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplinaServiceImpl implements DisciplinaService {

    private final DisciplinaRepository repo;

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<DisciplinaDto>> listar() {
        var data = repo.findAll().stream().map(this::toDto).toList();
        return new BaseResponse<>("OK", 200, data);
    }

    @Override
    @Transactional
    public BaseResponse<DisciplinaDto> crear(DisciplinaCreateDto dto) {
        String nombre = dto.getNombre().trim().toUpperCase();

        if (repo.existsByNombreIgnoreCase(nombre)) {
            return new BaseResponse<>("La disciplina ya existe", 400, null);
        }

        DisciplinaEntity d = DisciplinaEntity.builder()
                .nombre(nombre)
                .activa(true)
                .build();

        d = repo.save(d);
        return new BaseResponse<>("Disciplina creada", 200, toDto(d));
    }

    @Override
    @Transactional
    public BaseResponse<DisciplinaDto> actualizar(Long id, DisciplinaCreateDto dto) {
        DisciplinaEntity d = repo.findById(id).orElse(null);
        if (d == null) return new BaseResponse<>("Disciplina no encontrada", 404, null);

        String nombre = dto.getNombre().trim().toUpperCase();

        if (!d.getNombre().equalsIgnoreCase(nombre) && repo.existsByNombreIgnoreCase(nombre)) {
            return new BaseResponse<>("Ya existe otra disciplina con ese nombre", 400, null);
        }

        d.setNombre(nombre);
        d = repo.save(d);
        return new BaseResponse<>("Disciplina actualizada", 200, toDto(d));
    }

    @Override
    @Transactional
    public BaseResponse<DisciplinaDto> cambiarEstado(Long id, boolean activa) {
        DisciplinaEntity d = repo.findById(id).orElse(null);
        if (d == null) return new BaseResponse<>("Disciplina no encontrada", 404, null);

        d.setActiva(activa);
        d = repo.save(d);
        return new BaseResponse<>("Estado actualizado", 200, toDto(d));
    }

    private DisciplinaDto toDto(DisciplinaEntity e) {
        return DisciplinaDto.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .activa(e.getActiva())
                .build();
    }
}
