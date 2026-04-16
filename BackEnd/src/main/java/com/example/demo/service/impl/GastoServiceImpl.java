package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.gasto.*;
import com.example.demo.entity.GastoCategoria;
import com.example.demo.entity.GastoEntity;
import com.example.demo.entity.MedioPagoGasto;
import com.example.demo.mapper.GastoMapper;
import com.example.demo.repository.GastoRepository;
import com.example.demo.service.GastoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoServiceImpl implements GastoService {

        private final GastoRepository gastoRepository;

        @Override
        public BaseResponse<GastoDto> crear(GastoCreateDto dto) {

                if (dto.getCategoria() == null) {
                        return new BaseResponse<>("La categoría es obligatoria", 400, null);
                }

                if (dto.getConcepto() == null || dto.getConcepto().trim().isBlank()) {
                        return new BaseResponse<>("El concepto es obligatorio", 400, null);
                }

                if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                        return new BaseResponse<>("El monto debe ser mayor a 0", 400, null);
                }

                GastoEntity gasto = GastoEntity.builder()
                                .fecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now())
                                .categoria(dto.getCategoria())
                                .concepto(dto.getConcepto().trim())
                                .descripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null)
                                .monto(dto.getMonto())
                                .medioPago(
                                                dto.getMedioPago() != null
                                                                ? MedioPagoGasto.valueOf(
                                                                                dto.getMedioPago().trim().toUpperCase())
                                                                : null)
                                .activo(true)
                                .build();

                gasto = gastoRepository.save(gasto);

                return new BaseResponse<>("Gasto creado correctamente", 201, GastoMapper.toDto(gasto));
        }

        @Override
        public BaseResponse<List<GastoDto>> listar(
                        String categoria,
                        String concepto,
                        LocalDate fechaDesde,
                        LocalDate fechaHasta,
                        String q) {
                GastoCategoria categoriaEnum = parseCategoria(categoria);

                LocalDate desde = fechaDesde != null ? fechaDesde : LocalDate.of(2000, 1, 1);
                LocalDate hasta = fechaHasta != null ? fechaHasta : LocalDate.of(2999, 12, 31);
                String texto = normalizar(q);
                String conceptoNormalizado = normalizar(concepto);

                List<GastoDto> data = gastoRepository.buscar(categoriaEnum, conceptoNormalizado, desde, hasta, texto)
                                .stream()
                                .map(GastoMapper::toDto)
                                .toList();

                return new BaseResponse<>("Gastos obtenidos correctamente", 200, data);
        }

        @Override
        public BaseResponse<DashboardGastosResponseDto> dashboard(
                        String categoria,
                        String concepto,
                        LocalDate fechaDesde,
                        LocalDate fechaHasta,
                        String q) {
                GastoCategoria categoriaEnum = parseCategoria(categoria);
                String texto = normalizar(q);
                String conceptoNormalizado = normalizar(concepto);

                boolean hayFiltroFecha = fechaDesde != null || fechaHasta != null;
                boolean hayOtrosFiltros = (categoria != null && !categoria.isBlank()) ||
                                (concepto != null && !concepto.isBlank()) ||
                                (texto != null && !texto.isBlank());

                LocalDate hoy = LocalDate.now();

                LocalDate desde = hayFiltroFecha
                                ? (fechaDesde != null ? fechaDesde : LocalDate.of(2000, 1, 1))
                                : (hayOtrosFiltros ? LocalDate.of(2000, 1, 1) : hoy.withDayOfMonth(1));

                LocalDate hasta = hayFiltroFecha
                                ? (fechaHasta != null ? fechaHasta : LocalDate.of(2999, 12, 31))
                                : (hayOtrosFiltros ? LocalDate.of(2999, 12, 31)
                                                : hoy.withDayOfMonth(hoy.lengthOfMonth()));

                List<GastoDto> gastos = gastoRepository.buscar(categoriaEnum, conceptoNormalizado, desde, hasta, texto)
                                .stream()
                                .map(GastoMapper::toDto)
                                .toList();

                BigDecimal total = gastoRepository.totalDashboardFiltrado(
                                categoriaEnum, conceptoNormalizado, desde, hasta, texto);
                Long cantidad = gastoRepository.cantidadDashboardFiltrado(
                                categoriaEnum, conceptoNormalizado, desde, hasta, texto);

                List<String> ranking = gastoRepository.categoriaMayorGastoDashboardFiltrado(
                                categoriaEnum, conceptoNormalizado, desde, hasta, texto);
                String categoriaMayor = ranking.isEmpty() ? "-" : ranking.get(0);

                DashboardGastosResumenDto resumen = DashboardGastosResumenDto.builder()
                                .totalMes(total)
                                .cantidadGastosMes(cantidad)
                                .categoriaMayorGasto(categoriaMayor)
                                .build();

                DashboardGastosResponseDto response = DashboardGastosResponseDto.builder()
                                .resumen(resumen)
                                .gastos(gastos)
                                .build();

                return new BaseResponse<>("Dashboard de gastos obtenido correctamente", 200, response);
        }

        @Override
        public BaseResponse<GastoDto> actualizar(Long id, GastoUpdateRequestDto request) {
                GastoEntity gasto = gastoRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("No se encontró el gasto con id: " + id));

                gasto.setFecha(request.getFecha());
                gasto.setCategoria(request.getCategoria());
                gasto.setConcepto(request.getConcepto());
                gasto.setDescripcion(request.getDescripcion());
                gasto.setMedioPago(request.getMedioPago());
                gasto.setMonto(request.getMonto());

                GastoEntity actualizado = gastoRepository.save(gasto);

                GastoDto dto = GastoDto.builder()
                                .id(actualizado.getId())
                                .fecha(actualizado.getFecha())
                                .categoria(actualizado.getCategoria())
                                .concepto(actualizado.getConcepto())
                                .descripcion(actualizado.getDescripcion())
                                .medioPago(actualizado.getMedioPago())
                                .monto(actualizado.getMonto())
                                .build();

                return BaseResponse.<GastoDto>builder()
                                .data(dto)
                                .mensaje("Gasto actualizado correctamente")
                                .status(200)
                                .build();
        }

        private GastoCategoria parseCategoria(String categoria) {
                if (categoria == null || categoria.isBlank())
                        return null;
                try {
                        return GastoCategoria.valueOf(categoria.trim().toUpperCase());
                } catch (Exception e) {
                        return null;
                }
        }

        private String normalizar(String texto) {
                if (texto == null || texto.trim().isBlank())
                        return "";
                return texto.trim();
        }
}