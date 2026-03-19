package com.example.demo.repository;

import com.example.demo.entity.CategoriaIngresoManual;
import com.example.demo.entity.IngresoManualEntity;
import com.example.demo.entity.MedioIngresoManual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IngresoManualRepository extends JpaRepository<IngresoManualEntity, Long> {

    @Query("""
        SELECT i
        FROM IngresoManualEntity i
        WHERE i.fecha >= :fechaDesde
          AND i.fecha <= :fechaHasta
          AND (:categoria IS NULL OR i.categoria = :categoria)
          AND (
            :q = '' OR
            lower(cast(i.categoria as string)) LIKE concat('%', lower(:q), '%') OR
            lower(coalesce(i.descripcion, '')) LIKE concat('%', lower(:q), '%')
          )
        ORDER BY i.fecha DESC, i.id DESC
    """)
    List<IngresoManualEntity> buscarDashboard(
            @Param("categoria") CategoriaIngresoManual categoria,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("q") String q
    );

    @Query("""
        SELECT COALESCE(SUM(i.monto), 0)
        FROM IngresoManualEntity i
        WHERE (:categoria IS NULL OR i.categoria = :categoria)
          AND i.fecha >= :fechaDesde
          AND i.fecha <= :fechaHasta
          AND (
            :q = '' OR
            lower(cast(i.categoria as string)) LIKE concat('%', lower(:q), '%') OR
            lower(coalesce(i.descripcion, '')) LIKE concat('%', lower(:q), '%')
          )
    """)
    BigDecimal totalDashboardFiltrado(
            @Param("categoria") CategoriaIngresoManual categoria,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("q") String q
    );

    @Query("""
        SELECT COUNT(i)
        FROM IngresoManualEntity i
        WHERE (:categoria IS NULL OR i.categoria = :categoria)
          AND i.fecha >= :fechaDesde
          AND i.fecha <= :fechaHasta
          AND (
            :q = '' OR
            lower(cast(i.categoria as string)) LIKE concat('%', lower(:q), '%') OR
            lower(coalesce(i.descripcion, '')) LIKE concat('%', lower(:q), '%')
          )
    """)
    Long cantidadDashboardFiltrado(
            @Param("categoria") CategoriaIngresoManual categoria,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("q") String q
    );

    @Query("""
        SELECT COALESCE(SUM(i.monto), 0)
        FROM IngresoManualEntity i
        WHERE YEAR(i.fecha) = :anio
          AND MONTH(i.fecha) = :mes
    """)
    BigDecimal totalMes(@Param("anio") int anio, @Param("mes") int mes);

    @Query("""
        SELECT COUNT(i)
        FROM IngresoManualEntity i
        WHERE YEAR(i.fecha) = :anio
          AND MONTH(i.fecha) = :mes
    """)
    Long cantidadMes(@Param("anio") int anio, @Param("mes") int mes);

    @Query("""
        SELECT i.medioPago
        FROM IngresoManualEntity i
        WHERE YEAR(i.fecha) = :anio
          AND MONTH(i.fecha) = :mes
        GROUP BY i.medioPago
        ORDER BY COUNT(i) DESC
    """)
    List<MedioIngresoManual> medioMasUsado(@Param("anio") int anio, @Param("mes") int mes);

    @Query("""
    SELECT i
    FROM IngresoManualEntity i
    WHERE i.categoria = :categoria
      AND (:medio IS NULL OR i.medioPago = :medio)
      AND i.fecha >= :fechaDesde
      AND i.fecha <= :fechaHasta
      AND (
        :q = '' OR
        lower(cast(i.categoria as string)) LIKE concat('%', lower(:q), '%') OR
        lower(coalesce(i.descripcion, '')) LIKE concat('%', lower(:q), '%')
      )
    ORDER BY i.fecha DESC
""")
    List<IngresoManualEntity> buscarDashboardPorCategoria(
            @Param("categoria") CategoriaIngresoManual categoria,
            @Param("medio") MedioIngresoManual medio,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("q") String q
    );
}