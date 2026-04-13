package com.example.demo.repository;

import com.example.demo.entity.SocioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocioRepository extends JpaRepository<SocioEntity, Long> {

  Optional<SocioEntity> findByDni(String dni);

  @EntityGraph(attributePaths = { "socioDisciplinas", "socioDisciplinas.disciplina",
      "socioDisciplinas.arancelDisciplina" })
  @Query("""
          SELECT DISTINCT s
          FROM SocioEntity s
          LEFT JOIN s.socioDisciplinas sd
          LEFT JOIN sd.arancelDisciplina ad
          WHERE (:disciplinaId IS NULL OR (sd.activo = true AND sd.disciplina.id = :disciplinaId))
            AND (:categoria IS NULL OR (sd.activo = true AND ad.categoria = :categoria))
            AND (
                  :q IS NULL
                  OR LOWER(s.apellido) LIKE CONCAT('%', CAST(:q AS string), '%')
                  OR LOWER(s.nombre) LIKE CONCAT('%', CAST(:q AS string), '%')
                  OR s.dni LIKE CONCAT('%', CAST(:q AS string), '%')
            )
          ORDER BY s.id DESC
      """)
  List<SocioEntity> search(
      @Param("disciplinaId") Long disciplinaId,
      @Param("categoria") String categoria,
      @Param("q") String q);

  @EntityGraph(attributePaths = { "socioDisciplinas", "socioDisciplinas.disciplina",
      "socioDisciplinas.arancelDisciplina" })
  @Query("""
                      SELECT DISTINCT s
                      FROM SocioEntity s
                      LEFT JOIN s.socioDisciplinas sd
                      LEFT JOIN sd.arancelDisciplina ad
                      WHERE (:disciplinaId IS NULL OR (sd.activo = true AND sd.disciplina.id = :disciplinaId))
                        AND (:activo IS NULL OR s.activo = :activo)
                        AND (:categoria IS NULL OR (sd.activo = true AND ad.categoria = :categoria))
                       AND (
          :q IS NULL
          OR LOWER(s.apellido) LIKE CONCAT('%', CAST(:q AS string), '%')
          OR LOWER(s.nombre) LIKE CONCAT('%', CAST(:q AS string), '%')
          OR s.dni LIKE CONCAT('%', CAST(:q AS string), '%')
      )
                      ORDER BY s.apellido ASC, s.nombre ASC
                  """)
  List<SocioEntity> searchDashboard(
      @Param("disciplinaId") Long disciplinaId,
      @Param("activo") Boolean activo,
      @Param("categoria") String categoria,
      @Param("q") String q);

  @Query("""
          SELECT COUNT(DISTINCT s.id)
          FROM SocioEntity s
          LEFT JOIN s.socioDisciplinas sd
          WHERE sd.activo = true
            AND sd.disciplina.id = :disciplinaId
      """)
  long countByDisciplinaId(@Param("disciplinaId") Long disciplinaId);

  @Query("""
          SELECT COUNT(s)
          FROM SocioEntity s
          WHERE s.activo = true
      """)
  long countActivos();

  @Query("""
          SELECT COUNT(s)
          FROM SocioEntity s
          WHERE s.activo = false
      """)
  long countInactivos();
}