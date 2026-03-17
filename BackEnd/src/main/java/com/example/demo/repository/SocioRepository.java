package com.example.demo.repository;

import com.example.demo.entity.SocioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SocioRepository extends JpaRepository<SocioEntity, Long> {

    Optional<SocioEntity> findByDni(String dni);

    @Query("""
        SELECT s FROM SocioEntity s
        WHERE (:disciplinaId IS NULL OR s.disciplina.id = :disciplinaId)
          AND (
            :qLower IS NULL OR
            lower(s.apellido) LIKE concat('%', cast(:qLower as string), '%') OR
            lower(s.nombre) LIKE concat('%', cast(:qLower as string), '%') OR
            s.dni LIKE concat('%', cast(:qRaw as string), '%')
          )
          AND (
            :estadoPago IS NULL OR
            (:estadoPago = 'AL_DIA' AND s.vigenciaHasta IS NOT NULL AND s.vigenciaHasta >= :hoy) OR
            (:estadoPago = 'DEBE' AND (s.vigenciaHasta IS NULL OR s.vigenciaHasta < :hoy))
          )
              ORDER BY s.id DESC
    """)
    List<SocioEntity> search(
            @Param("disciplinaId") Long disciplinaId,
            @Param("estadoPago") String estadoPago,
            @Param("qLower") String qLower,
            @Param("qRaw") String qRaw,
            @Param("hoy") LocalDate hoy
    );

    long countByDisciplinaId(Long disciplinaId);

    @Query("""
        SELECT COUNT(s)
        FROM SocioEntity s
        WHERE s.vigenciaHasta IS NOT NULL
          AND s.vigenciaHasta >= :hoy
    """)
    long countAlDia(@Param("hoy") LocalDate hoy);

    @Query("""
        SELECT COUNT(s)
        FROM SocioEntity s
        WHERE s.vigenciaHasta IS NULL
           OR s.vigenciaHasta < :hoy
    """)
    long countDebe(@Param("hoy") LocalDate hoy);

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

    @Query("""
        SELECT s FROM SocioEntity s
        WHERE (:disciplinaId IS NULL OR s.disciplina.id = :disciplinaId)
          AND (:activo IS NULL OR s.activo = :activo)
          AND (
            :estadoPago IS NULL OR
            (:estadoPago = 'AL_DIA' AND s.vigenciaHasta IS NOT NULL AND s.vigenciaHasta >= :hoy) OR
            (:estadoPago = 'DEBE' AND (s.vigenciaHasta IS NULL OR s.vigenciaHasta < :hoy))
          )
          AND (
            :qLower IS NULL OR
            lower(s.apellido) LIKE concat('%', cast(:qLower as string), '%') OR
            lower(s.nombre) LIKE concat('%', cast(:qLower as string), '%') OR
            s.dni LIKE concat('%', cast(:qRaw as string), '%')
          )
        ORDER BY s.apellido ASC, s.nombre ASC
    """)
    List<SocioEntity> searchDashboard(
            @Param("disciplinaId") Long disciplinaId,
            @Param("activo") Boolean activo,
            @Param("estadoPago") String estadoPago,
            @Param("qLower") String qLower,
            @Param("qRaw") String qRaw,
            @Param("hoy") LocalDate hoy
    );
}