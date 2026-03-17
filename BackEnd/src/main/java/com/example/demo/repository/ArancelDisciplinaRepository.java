package com.example.demo.repository;

import com.example.demo.entity.ArancelDisciplinaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ArancelDisciplinaRepository extends JpaRepository<ArancelDisciplinaEntity, Long> {

    List<ArancelDisciplinaEntity> findByActivaTrueOrderByDisciplina_IdAscCategoriaAsc();

    List<ArancelDisciplinaEntity> findByDisciplina_IdAndActivaTrueOrderByCategoriaAsc(Long disciplinaId);

    Optional<ArancelDisciplinaEntity> findTopByDisciplina_IdAndCategoriaIgnoreCaseAndActivaTrueOrderByVigenteDesdeDesc(
            Long disciplinaId,
            String categoria
    );

    Optional<ArancelDisciplinaEntity> findTopByDisciplina_IdAndVigenteDesdeLessThanEqualAndActivaTrueOrderByVigenteDesdeDesc(
            Long disciplinaId,
            LocalDate fecha
    );

    Optional<ArancelDisciplinaEntity> findTopByDisciplina_IdAndCategoriaIgnoreCaseAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
            Long disciplinaId,
            String categoria,
            LocalDate fecha
    );

    Optional<ArancelDisciplinaEntity> findTopByDisciplina_IdAndCategoriaIgnoreCaseOrderByVigenteDesdeAsc(
            Long disciplinaId,
            String categoria
    );
}