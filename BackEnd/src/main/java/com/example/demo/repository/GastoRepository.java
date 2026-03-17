package com.example.demo.repository;

import com.example.demo.entity.GastoCategoria;
import com.example.demo.entity.GastoEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GastoRepository extends JpaRepository<GastoEntity, Long> {

    @Query("""
    SELECT g
    FROM GastoEntity g
    WHERE g.activo = true
      AND (:categoria IS NULL OR g.categoria = :categoria)
      AND g.fecha >= :fechaDesde
      AND g.fecha <= :fechaHasta
      AND (
        :q = '' OR
        lower(g.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(coalesce(g.descripcion, '')) LIKE concat('%', lower(cast(:q as string)), '%')
      )
    ORDER BY g.fecha DESC, g.id DESC
""")
    List<GastoEntity> buscar(
            @Param("categoria") GastoCategoria categoria,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("q") String q
    );

    @Query("""
        SELECT COALESCE(SUM(g.monto), 0)
        FROM GastoEntity g
        WHERE g.activo = true
          AND (:categoria IS NULL OR g.categoria = :categoria)
          AND g.fecha >= :fechaDesde
          AND g.fecha <= :fechaHasta
          AND (
            :q = '' OR
            lower(g.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(coalesce(g.descripcion, '')) LIKE concat('%', lower(cast(:q as string)), '%')
          )
    """)
    BigDecimal totalDashboardFiltrado(
            @Param("categoria") GastoCategoria categoria,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("q") String q
    );

    @Query("""
        SELECT COUNT(g)
        FROM GastoEntity g
        WHERE g.activo = true
          AND (:categoria IS NULL OR g.categoria = :categoria)
          AND g.fecha >= :fechaDesde
          AND g.fecha <= :fechaHasta
          AND (
            :q = '' OR
            lower(g.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(coalesce(g.descripcion, '')) LIKE concat('%', lower(cast(:q as string)), '%')
          )
    """)
    Long cantidadDashboardFiltrado(
            @Param("categoria") GastoCategoria categoria,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("q") String q
    );

    @Query("""
        SELECT cast(g.categoria as string)
        FROM GastoEntity g
        WHERE g.activo = true
          AND (:categoria IS NULL OR g.categoria = :categoria)
          AND g.fecha >= :fechaDesde
          AND g.fecha <= :fechaHasta
          AND (
            :q = '' OR
            lower(g.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(coalesce(g.descripcion, '')) LIKE concat('%', lower(cast(:q as string)), '%')
          )
        GROUP BY g.categoria
        ORDER BY SUM(g.monto) DESC
    """)
    List<String> categoriaMayorGastoDashboardFiltrado(
            @Param("categoria") GastoCategoria categoria,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("q") String q
    );

    @Query("""
        SELECT COALESCE(SUM(g.monto), 0)
        FROM GastoEntity g
        WHERE g.activo = true
          AND YEAR(g.fecha) = :anio
          AND MONTH(g.fecha) = :mes
    """)
    BigDecimal totalMes(@Param("anio") int anio, @Param("mes") int mes);

    @Query("""
        SELECT COUNT(g)
        FROM GastoEntity g
        WHERE g.activo = true
          AND YEAR(g.fecha) = :anio
          AND MONTH(g.fecha) = :mes
    """)
    Long cantidadMes(@Param("anio") int anio, @Param("mes") int mes);

    @Query("""
        SELECT g.categoria
        FROM GastoEntity g
        WHERE g.activo = true
          AND YEAR(g.fecha) = :anio
          AND MONTH(g.fecha) = :mes
        GROUP BY g.categoria
        ORDER BY SUM(g.monto) DESC
    """)
    List<GastoCategoria> categoriaMayorGasto(@Param("anio") int anio, @Param("mes") int mes);
}