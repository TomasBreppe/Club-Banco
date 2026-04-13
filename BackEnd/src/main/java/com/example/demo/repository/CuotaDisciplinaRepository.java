package com.example.demo.repository;

import com.example.demo.entity.CuotaDisciplinaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CuotaDisciplinaRepository extends JpaRepository<CuotaDisciplinaEntity, Long> {
    Optional<CuotaDisciplinaEntity> findTopByDisciplina_IdAndActivaTrueOrderByVigenteDesdeDesc(Long disciplinaId);

    default Optional<CuotaDisciplinaEntity> getActiva(Long disciplinaId) {
        return findTopByDisciplina_IdAndActivaTrueOrderByVigenteDesdeDesc(disciplinaId);
    }

    Optional<CuotaDisciplinaEntity> findTopByDisciplina_IdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
            Long disciplinaId,
            LocalDate fecha
    );
    Optional<CuotaDisciplinaEntity> findTopByDisciplina_IdOrderByVigenteDesdeAsc(Long disciplinaId);

}