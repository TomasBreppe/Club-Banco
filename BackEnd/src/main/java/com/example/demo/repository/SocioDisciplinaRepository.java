package com.example.demo.repository;

import com.example.demo.entity.SocioDisciplinaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocioDisciplinaRepository extends JpaRepository<SocioDisciplinaEntity, Long> {

    @EntityGraph(attributePaths = {"disciplina", "arancelDisciplina"})
    List<SocioDisciplinaEntity> findBySocio_IdAndActivoTrue(Long socioId);

    @EntityGraph(attributePaths = {"disciplina", "arancelDisciplina"})
    List<SocioDisciplinaEntity> findBySocio_IdOrderByIdAsc(Long socioId);

    @EntityGraph(attributePaths = {"disciplina", "arancelDisciplina"})
    Optional<SocioDisciplinaEntity> findBySocio_IdAndDisciplina_IdAndActivoTrue(Long socioId, Long disciplinaId);

    @EntityGraph(attributePaths = {"disciplina", "arancelDisciplina"})
    Optional<SocioDisciplinaEntity> findFirstBySocio_IdAndActivoTrueOrderByIdAsc(Long socioId);

    @EntityGraph(attributePaths = {"disciplina", "arancelDisciplina"})
    List<SocioDisciplinaEntity> findBySocio_IdInAndActivoTrueOrderBySocio_IdAscIdAsc(List<Long> socioIds);

    long countBySocio_IdAndActivoTrue(Long socioId);
}