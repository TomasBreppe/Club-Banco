package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.balance.DashboardBalanceResponseDto;
import com.example.demo.dto.balance.DashboardBalanceResumenDto;
import com.example.demo.repository.GastoRepository;
import com.example.demo.repository.IngresoManualRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.service.DashboardBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DashboardBalanceServiceImpl implements DashboardBalanceService {

    private final PagoRepository pagoRepository;
    private final IngresoManualRepository ingresoManualRepository;
    private final GastoRepository gastoRepository;

    @Override
    public BaseResponse<DashboardBalanceResponseDto> dashboard(LocalDate fechaDesde, LocalDate fechaHasta) {

        boolean hayFiltroFecha = fechaDesde != null || fechaHasta != null;

        LocalDate hoy = LocalDate.now();

        LocalDate desde = hayFiltroFecha
                ? (fechaDesde != null ? fechaDesde : LocalDate.of(2000, 1, 1))
                : hoy.withDayOfMonth(1);

        LocalDate hasta = hayFiltroFecha
                ? (fechaHasta != null ? fechaHasta : LocalDate.of(2999, 12, 31))
                : hoy.withDayOfMonth(hoy.lengthOfMonth());

        LocalDateTime desdeDateTime = desde.atStartOfDay();
        LocalDateTime hastaDateTime = hasta.atTime(LocalTime.MAX);

        BigDecimal ingresosCuotas = pagoRepository.totalDashboardFiltradoSinMedio(
                desdeDateTime,
                hastaDateTime,
                ""
        );

        BigDecimal ingresosManuales = ingresoManualRepository.totalDashboardFiltradoSinMedio(
                desde,
                hasta,
                ""
        );

        BigDecimal gastos = gastoRepository.totalDashboardFiltrado(
                null,
                desde,
                hasta,
                ""
        );

        BigDecimal ingresosTotales = ingresosCuotas.add(ingresosManuales);
        BigDecimal neto = ingresosTotales.subtract(gastos);

        DashboardBalanceResumenDto resumen = DashboardBalanceResumenDto.builder()
                .ingresosMes(ingresosTotales)
                .gastosMes(gastos)
                .netoMes(neto)
                .build();

        DashboardBalanceResponseDto response = DashboardBalanceResponseDto.builder()
                .resumen(resumen)
                .build();

        return new BaseResponse<>("Dashboard de balance obtenido correctamente", 200, response);
    }
}