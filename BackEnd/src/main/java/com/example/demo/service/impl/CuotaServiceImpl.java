package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.cuota.*;
import com.example.demo.entity.CuotaDisciplinaEntity;
import com.example.demo.entity.DisciplinaEntity;
import com.example.demo.repository.CuotaDisciplinaRepository;
import com.example.demo.repository.DisciplinaRepository;
import com.example.demo.service.CuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CuotaServiceImpl implements CuotaService {

    private final CuotaDisciplinaRepository cuotaRepo;
    private final DisciplinaRepository disciplinaRepo;

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<CuotaDto> getActiva(Long disciplinaId) {
        var cuota = cuotaRepo.findTopByDisciplina_IdAndActivaTrueOrderByVigenteDesdeDesc(disciplinaId).orElse(null);
        if (cuota == null) return new BaseResponse<>("No hay cuota activa", 404, null);
        return new BaseResponse<>("OK", 200, toDto(cuota));
    }

    @Override
    @Transactional
    public BaseResponse<CuotaDto> crearNueva(Long disciplinaId, CuotaCreateDto dto) {
        DisciplinaEntity d = disciplinaRepo.findById(disciplinaId).orElse(null);
        if (d == null) return new BaseResponse<>("Disciplina no encontrada", 404, null);

        // Desactivar cuota activa anterior (si existe)
        cuotaRepo.findTopByDisciplina_IdAndActivaTrueOrderByVigenteDesdeDesc(disciplinaId)
                .ifPresent(old -> {
                    old.setActiva(false);
                    cuotaRepo.save(old);
                });

        CuotaDisciplinaEntity nueva = CuotaDisciplinaEntity.builder()
                .disciplina(d)
                .montoTotal(dto.getMontoTotal())
                .vigenteDesde(dto.getVigenteDesde() != null ? dto.getVigenteDesde() : LocalDate.now())
                .activa(true)
                .build();

        nueva = cuotaRepo.save(nueva);
        return new BaseResponse<>("Cuota creada", 200, toDto(nueva));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<CuotaDto>> historial(Long disciplinaId) {
        // Simple: traemos todas y filtramos por disciplina (si querés, hacemos query específica)
        var data = cuotaRepo.findAll().stream()
                .filter(c -> c.getDisciplina().getId().equals(disciplinaId))
                .sorted((a,b) -> b.getVigenteDesde().compareTo(a.getVigenteDesde()))
                .map(this::toDto)
                .toList();

        return new BaseResponse<>("OK", 200, data);
    }

    private CuotaDto toDto(CuotaDisciplinaEntity e) {
        return CuotaDto.builder()
                .id(e.getId())
                .disciplinaId(e.getDisciplina().getId())
                .montoTotal(e.getMontoTotal())
                .vigenteDesde(e.getVigenteDesde())
                .activa(e.getActiva())
                .build();
    }
}
