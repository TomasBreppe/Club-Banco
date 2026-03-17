package com.example.demo.repository;

import com.example.demo.entity.MedioPago;
import com.example.demo.entity.PagoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<PagoEntity, Long> {

    Optional<PagoEntity> findByMpPaymentId(String mpPaymentId);

    boolean existsBySocio_IdAndConceptoAndPeriodo(Long socioId, String concepto, String periodo);

    boolean existsBySocio_IdAndConcepto(Long socioId, String concepto);

    List<PagoEntity> findBySocio_IdAndConceptoAndPeriodoIn(Long socioId, String concepto, List<String> periodos);

    List<PagoEntity> findBySocio_IdOrderByFechaPagoDesc(Long socioId);

    List<PagoEntity> findBySocio_IdAndFechaPagoBetweenOrderByFechaPagoDesc(
            Long socioId, LocalDateTime desde, LocalDateTime hasta);

    List<PagoEntity> findBySocio_IdOrderByFechaPagoDesc(Long socioId, Pageable pageable);

    @Query("""
        SELECT p
        FROM PagoEntity p
        WHERE p.fechaPago >= :fechaDesde
          AND p.fechaPago <= :fechaHasta
          AND (
            lower(p.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(coalesce(p.periodo, '')) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(p.socio.nombre) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(p.socio.apellido) LIKE concat('%', lower(cast(:q as string)), '%') OR
            p.socio.dni LIKE concat('%', cast(:q as string), '%')
          )
        ORDER BY p.fechaPago DESC, p.id DESC
    """)
    List<PagoEntity> buscarDashboardSinMedio(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("q") String q
    );

    @Query("""
        SELECT p
        FROM PagoEntity p
        WHERE p.medio = :medio
          AND p.fechaPago >= :fechaDesde
          AND p.fechaPago <= :fechaHasta
          AND (
            lower(p.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(coalesce(p.periodo, '')) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(p.socio.nombre) LIKE concat('%', lower(cast(:q as string)), '%') OR
            lower(p.socio.apellido) LIKE concat('%', lower(cast(:q as string)), '%') OR
            p.socio.dni LIKE concat('%', cast(:q as string), '%')
          )
        ORDER BY p.fechaPago DESC, p.id DESC
    """)
    List<PagoEntity> buscarDashboardConMedio(
            @Param("medio") MedioPago medio,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("q") String q
    );


    @Query("""
    SELECT COALESCE(SUM(p.montoTotal), 0)
    FROM PagoEntity p
    WHERE p.fechaPago >= :fechaDesde
      AND p.fechaPago <= :fechaHasta
      AND (
        :q = '' OR
        lower(p.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(coalesce(p.periodo, '')) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(p.socio.nombre) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(p.socio.apellido) LIKE concat('%', lower(cast(:q as string)), '%') OR
        p.socio.dni LIKE concat('%', cast(:q as string), '%')
      )
""")
    BigDecimal totalDashboardFiltradoSinMedio(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("q") String q
    );

    @Query("""
    SELECT COUNT(p)
    FROM PagoEntity p
    WHERE p.fechaPago >= :fechaDesde
      AND p.fechaPago <= :fechaHasta
      AND (
        :q = '' OR
        lower(p.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(coalesce(p.periodo, '')) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(p.socio.nombre) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(p.socio.apellido) LIKE concat('%', lower(cast(:q as string)), '%') OR
        p.socio.dni LIKE concat('%', cast(:q as string), '%')
      )
""")
    Long cantidadDashboardFiltradoSinMedio(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("q") String q
    );

    @Query("""
    SELECT COALESCE(SUM(p.montoTotal), 0)
    FROM PagoEntity p
    WHERE p.medio = :medio
      AND p.fechaPago >= :fechaDesde
      AND p.fechaPago <= :fechaHasta
      AND (
        :q = '' OR
        lower(p.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(coalesce(p.periodo, '')) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(p.socio.nombre) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(p.socio.apellido) LIKE concat('%', lower(cast(:q as string)), '%') OR
        p.socio.dni LIKE concat('%', cast(:q as string), '%')
      )
""")
    BigDecimal totalDashboardFiltradoConMedio(
            @Param("medio") MedioPago medio,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("q") String q
    );

    @Query("""
    SELECT COUNT(p)
    FROM PagoEntity p
    WHERE p.medio = :medio
      AND p.fechaPago >= :fechaDesde
      AND p.fechaPago <= :fechaHasta
      AND (
        :q = '' OR
        lower(p.concepto) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(coalesce(p.periodo, '')) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(p.socio.nombre) LIKE concat('%', lower(cast(:q as string)), '%') OR
        lower(p.socio.apellido) LIKE concat('%', lower(cast(:q as string)), '%') OR
        p.socio.dni LIKE concat('%', cast(:q as string), '%')
      )
""")
    Long cantidadDashboardFiltradoConMedio(
            @Param("medio") MedioPago medio,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("q") String q
    );


    @Query("""
        SELECT COALESCE(SUM(p.montoTotal), 0)
        FROM PagoEntity p
        WHERE YEAR(p.fechaPago) = :anio
          AND MONTH(p.fechaPago) = :mes
    """)
    BigDecimal totalMes(@Param("anio") int anio, @Param("mes") int mes);

    @Query("""
        SELECT COUNT(p)
        FROM PagoEntity p
        WHERE YEAR(p.fechaPago) = :anio
          AND MONTH(p.fechaPago) = :mes
    """)
    Long cantidadMes(@Param("anio") int anio, @Param("mes") int mes);

    @Query("""
        SELECT p.medio
        FROM PagoEntity p
        WHERE YEAR(p.fechaPago) = :anio
          AND MONTH(p.fechaPago) = :mes
        GROUP BY p.medio
        ORDER BY COUNT(p) DESC
    """)
    List<MedioPago> medioMasUsado(@Param("anio") int anio, @Param("mes") int mes);
}