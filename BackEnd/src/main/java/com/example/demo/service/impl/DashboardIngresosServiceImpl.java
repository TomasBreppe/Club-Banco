package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.ingresos.IngresoDashboardItemDto;
import com.example.demo.dto.pagos.DashboardIngresosResponseDto;
import com.example.demo.dto.pagos.DashboardIngresosResumenDto;
import com.example.demo.entity.CategoriaIngresoManual;
import com.example.demo.entity.IngresoManualEntity;
import com.example.demo.entity.PagoEntity;
import com.example.demo.repository.IngresoManualRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.service.DashboardIngresosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardIngresosServiceImpl implements DashboardIngresosService {

    private final PagoRepository pagoRepository;
    private final IngresoManualRepository ingresoManualRepository;

    @Override
    public BaseResponse<DashboardIngresosResponseDto> dashboard(
            String medio,
            Long disciplinaId,
            String categoriaManual,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String q
    ) {
        CategoriaIngresoManual categoriaManualEnum = parseCategoriaIngresoManual(categoriaManual);
        String texto = normalizar(q);

        boolean hayFiltroFecha = fechaDesde != null || fechaHasta != null;
        boolean hayOtrosFiltros =
                disciplinaId != null ||
                        categoriaManualEnum != null ||
                        (texto != null && !texto.isBlank());

        boolean filtroDisciplina = disciplinaId != null;
        boolean filtroCategoriaManual = categoriaManualEnum != null;

        LocalDate hoy = LocalDate.now();

        LocalDate desde = hayFiltroFecha
                ? (fechaDesde != null ? fechaDesde : LocalDate.of(2000, 1, 1))
                : (hayOtrosFiltros ? LocalDate.of(2000, 1, 1) : hoy.withDayOfMonth(1));

        LocalDate hasta = hayFiltroFecha
                ? (fechaHasta != null ? fechaHasta : LocalDate.of(2999, 12, 31))
                : (hayOtrosFiltros ? LocalDate.of(2999, 12, 31) : hoy.withDayOfMonth(hoy.lengthOfMonth()));

        LocalDateTime desdeDateTime = desde.atStartOfDay();
        LocalDateTime hastaDateTime = hasta.atTime(LocalTime.MAX);

        List<PagoEntity> pagos = new ArrayList<>();
        List<IngresoManualEntity> manuales = new ArrayList<>();

        // Si filtra por disciplina => SOLO cuotas
        if (filtroDisciplina) {
            pagos = pagoRepository.buscarDashboardPorDisciplina(
                    disciplinaId,
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );
        }
        // Si filtra por categoría manual => SOLO manuales
        else if (filtroCategoriaManual) {
            manuales = ingresoManualRepository.buscarDashboard(
                    categoriaManualEnum,
                    desde,
                    hasta,
                    texto == null ? "" : texto
            );
        }
        // Si no filtra ninguno => trae todo
        else {
            pagos = pagoRepository.buscarDashboardPorDisciplina(
                    null,
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );

            manuales = ingresoManualRepository.buscarDashboard(
                    null,
                    desde,
                    hasta,
                    texto == null ? "" : texto
            );
        }

        List<IngresoDashboardItemDto> items = new ArrayList<>();

        for (PagoEntity p : pagos) {
            items.add(IngresoDashboardItemDto.builder()
                    .origen("CUOTA")
                    .id(p.getId())
                    .fecha(p.getFechaPago())
                    .socioNombreCompleto(
                            p.getSocio() != null
                                    ? (p.getSocio().getNombre() + " " + p.getSocio().getApellido())
                                    : "-"
                    )
                    .disciplinaNombre(
                            p.getSocio() != null && p.getSocio().getDisciplina() != null
                                    ? p.getSocio().getDisciplina().getNombre()
                                    : null
                    )
                    .categoria("CUOTAS")
                    .concepto(p.getConcepto())
                    .periodo(p.getPeriodo())
                    .medio(p.getMedio() != null ? p.getMedio().name() : null)
                    .monto(p.getMontoTotal())
                    .descripcion(null)
                    .build());
        }

        for (IngresoManualEntity i : manuales) {
            items.add(IngresoDashboardItemDto.builder()
                    .origen("MANUAL")
                    .id(i.getId())
                    .fecha(i.getFecha().atStartOfDay())
                    .socioNombreCompleto("-")
                    .disciplinaNombre(null)
                    .categoria(i.getCategoria() != null ? i.getCategoria().name() : null)
                    .concepto(null)
                    .periodo(null)
                    .medio(i.getMedioPago() != null ? i.getMedioPago().name() : null)
                    .monto(i.getMonto())
                    .descripcion(i.getDescripcion())
                    .build());
        }

        items.sort(Comparator.comparing(IngresoDashboardItemDto::getFecha).reversed());

        BigDecimal totalCuotas = BigDecimal.ZERO;
        Long cantidadCuotas = 0L;
        BigDecimal totalManual = BigDecimal.ZERO;
        Long cantidadManual = 0L;

        if (filtroDisciplina) {
            totalCuotas = pagoRepository.totalDashboardFiltradoPorDisciplina(
                    disciplinaId,
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );

            cantidadCuotas = pagoRepository.cantidadDashboardFiltradoPorDisciplina(
                    disciplinaId,
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );
        } else if (filtroCategoriaManual) {
            totalManual = ingresoManualRepository.totalDashboardFiltrado(
                    categoriaManualEnum,
                    desde,
                    hasta,
                    texto == null ? "" : texto
            );

            cantidadManual = ingresoManualRepository.cantidadDashboardFiltrado(
                    categoriaManualEnum,
                    desde,
                    hasta,
                    texto == null ? "" : texto
            );
        } else {
            totalCuotas = pagoRepository.totalDashboardFiltradoPorDisciplina(
                    null,
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );

            cantidadCuotas = pagoRepository.cantidadDashboardFiltradoPorDisciplina(
                    null,
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );

            totalManual = ingresoManualRepository.totalDashboardFiltrado(
                    null,
                    desde,
                    hasta,
                    texto == null ? "" : texto
            );

            cantidadManual = ingresoManualRepository.cantidadDashboardFiltrado(
                    null,
                    desde,
                    hasta,
                    texto == null ? "" : texto
            );
        }

        BigDecimal total = totalCuotas.add(totalManual);
        Long cantidad = cantidadCuotas + cantidadManual;

        String medioMasUsado = calcularMedioMasUsado(items);

        DashboardIngresosResumenDto resumen = DashboardIngresosResumenDto.builder()
                .totalMes(total)
                .cantidadPagosMes(cantidad)
                .medioMasUsado(medioMasUsado)
                .build();

        DashboardIngresosResponseDto response = DashboardIngresosResponseDto.builder()
                .resumen(resumen)
                .ingresos(items)
                .build();

        return new BaseResponse<>("Dashboard de ingresos obtenido correctamente", 200, response);
    }

    private String calcularMedioMasUsado(List<IngresoDashboardItemDto> items) {
        return items.stream()
                .map(IngresoDashboardItemDto::getMedio)
                .filter(m -> m != null && !m.isBlank())
                .collect(java.util.stream.Collectors.groupingBy(m -> m, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("-");
    }

    private CategoriaIngresoManual parseCategoriaIngresoManual(String categoria) {
        if (categoria == null || categoria.isBlank()) return null;
        try {
            return CategoriaIngresoManual.valueOf(categoria.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizar(String q) {
        if (q == null || q.trim().isBlank()) return null;
        return q.trim();
    }
}