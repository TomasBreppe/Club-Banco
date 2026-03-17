package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.ingresos.IngresoDashboardItemDto;
import com.example.demo.dto.pagos.DashboardIngresosResponseDto;
import com.example.demo.dto.pagos.DashboardIngresosResumenDto;
import com.example.demo.entity.IngresoManualEntity;
import com.example.demo.entity.MedioIngresoManual;
import com.example.demo.entity.MedioPago;
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
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String q
    ) {
        MedioPago medioPagoCuota = parseMedioPago(medio);
        MedioIngresoManual medioIngresoManual = parseMedioIngresoManual(medio);

        String texto = normalizar(q);

        boolean hayFiltroFecha = fechaDesde != null || fechaHasta != null;
        boolean hayOtrosFiltros = (medio != null && !medio.isBlank()) || (texto != null && !texto.isBlank());

        LocalDate hoy = LocalDate.now();

        LocalDate desde = hayFiltroFecha
                ? (fechaDesde != null ? fechaDesde : LocalDate.of(2000, 1, 1))
                : (hayOtrosFiltros ? LocalDate.of(2000, 1, 1) : hoy.withDayOfMonth(1));

        LocalDate hasta = hayFiltroFecha
                ? (fechaHasta != null ? fechaHasta : LocalDate.of(2999, 12, 31))
                : (hayOtrosFiltros ? LocalDate.of(2999, 12, 31) : hoy.withDayOfMonth(hoy.lengthOfMonth()));

        LocalDateTime desdeDateTime = desde.atStartOfDay();
        LocalDateTime hastaDateTime = hasta.atTime(LocalTime.MAX);

        List<PagoEntity> pagos;
        if (medioPagoCuota == null) {
            pagos = pagoRepository.buscarDashboardSinMedio(desdeDateTime, hastaDateTime, texto == null ? "" : texto);
        } else {
            pagos = pagoRepository.buscarDashboardConMedio(medioPagoCuota, desdeDateTime, hastaDateTime, texto == null ? "" : texto);
        }

        List<IngresoManualEntity> manuales = ingresoManualRepository.buscarDashboard(
                medioIngresoManual,
                desde,
                hasta,
                texto == null ? "" : texto
        );

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

        BigDecimal totalCuotas;
        Long cantidadCuotas;

        if (medioPagoCuota == null) {
            totalCuotas = pagoRepository.totalDashboardFiltradoSinMedio(
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );

            cantidadCuotas = pagoRepository.cantidadDashboardFiltradoSinMedio(
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );
        } else {
            totalCuotas = pagoRepository.totalDashboardFiltradoConMedio(
                    medioPagoCuota,
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );

            cantidadCuotas = pagoRepository.cantidadDashboardFiltradoConMedio(
                    medioPagoCuota,
                    desdeDateTime,
                    hastaDateTime,
                    texto == null ? "" : texto
            );
        }

        BigDecimal totalManual = ingresoManualRepository.totalDashboardFiltrado(
                medioIngresoManual,
                desde,
                hasta,
                texto == null ? "" : texto
        );

        BigDecimal total = totalCuotas.add(totalManual);

        Long cantidadManual = ingresoManualRepository.cantidadDashboardFiltrado(
                medioIngresoManual,
                desde,
                hasta,
                texto == null ? "" : texto
        );

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

    private MedioPago parseMedioPago(String medio) {
        if (medio == null || medio.isBlank()) return null;
        try {
            return MedioPago.valueOf(medio.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private MedioIngresoManual parseMedioIngresoManual(String medio) {
        if (medio == null || medio.isBlank()) return null;
        try {
            return MedioIngresoManual.valueOf(medio.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizar(String q) {
        if (q == null || q.trim().isBlank()) return null;
        return q.trim();
    }
}