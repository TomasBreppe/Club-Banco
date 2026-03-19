package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.pagos.PagoDto;
import com.example.demo.dto.socio.SocioCreateDto;
import com.example.demo.dto.socio.SocioDto;
import com.example.demo.dto.socio.SocioResumenDto;
import com.example.demo.entity.DisciplinaEntity;
import com.example.demo.entity.Genero;
import com.example.demo.entity.SocioEntity;
import com.example.demo.mapper.SocioMapper;
import com.example.demo.repository.DisciplinaRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.SocioRepository;
import com.example.demo.service.FinanzasService;
import com.example.demo.service.SocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.example.demo.entity.ArancelDisciplinaEntity;
import com.example.demo.repository.ArancelDisciplinaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocioServiceImpl implements SocioService {

    private final SocioRepository socioRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final PagoRepository pagoRepository;
    private final FinanzasService finanzasService;
    private final ArancelDisciplinaRepository arancelDisciplinaRepository;

    @Override
    public BaseResponse<SocioDto> crear(SocioCreateDto dto) {

        if (socioRepository.findByDni(dto.getDni().trim()).isPresent()) {
            return BaseResponse.bad("Ya existe un socio con ese DNI");
        }

        if (dto.getArancelDisciplinaId() == null || dto.getArancelDisciplinaId() <= 0) {
            return BaseResponse.bad("Debés seleccionar una categoría/arancel");
        }

        DisciplinaEntity disc = disciplinaRepository.findById(dto.getDisciplinaId())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina inexistente"));

        ArancelDisciplinaEntity arancel = arancelDisciplinaRepository.findById(dto.getArancelDisciplinaId())
                .orElseThrow(() -> new IllegalArgumentException("Arancel inexistente"));

        if (!arancel.getDisciplina().getId().equals(disc.getId())) {
            return BaseResponse.bad("El arancel no corresponde a la disciplina seleccionada");
        }

        if (Boolean.FALSE.equals(arancel.getActiva())) {
            return BaseResponse.bad("El arancel seleccionado está inactivo");
        }

        SocioEntity socio = SocioEntity.builder()
                .dni(dto.getDni().trim())
                .nombre(dto.getNombre().trim())
                .apellido(dto.getApellido().trim())
                .genero(Genero.valueOf(dto.getGenero().trim().toUpperCase()))
                .telefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null)
                .celular(dto.getCelular().trim())
                .disciplina(disc)
                .arancelDisciplina(arancel)
                .activo(true)
                .vigenciaHasta(null)
                .inscripcionPagada(dto.getInscripcionPagada() != null ? dto.getInscripcionPagada() : false)
                .build();

        SocioEntity saved = socioRepository.save(socio);
        return BaseResponse.created("Socio creado", SocioMapper.toDto(saved));
    }

    @Override
    public BaseResponse<List<SocioDto>> listar(Long disciplinaId, String estadoPago, String q) {

        String qRaw = (q == null || q.isBlank()) ? null : q.trim();
        String qLower = (qRaw == null) ? null : qRaw.toLowerCase();

        String ep = (estadoPago == null || estadoPago.isBlank())
                ? null
                : estadoPago.trim().toUpperCase();

        LocalDate hoy = LocalDate.now();

        List<SocioDto> result = socioRepository.search(
                        disciplinaId,
                        ep,
                        qLower,
                        qRaw,
                        hoy
                ).stream()
                .map(s -> {
                    SocioDto dto = SocioMapper.toDto(s);

                    if (Boolean.FALSE.equals(s.getActivo())) {
                        dto.setEstadoPago("INACTIVO");
                        return dto;
                    }

                    LocalDate vigenciaHasta = s.getVigenciaHasta();

                    if (vigenciaHasta != null && !vigenciaHasta.isBefore(hoy)) {
                        dto.setEstadoPago("AL_DIA");
                    } else {
                        dto.setEstadoPago("DEBE");
                    }

                    return dto;
                })
                .toList();

        return BaseResponse.ok("OK", result);
    }

    // ✅ RESUMEN (para "Ver detalle")
    @Override
    public BaseResponse<SocioResumenDto> resumen(Long socioId) {

        SocioEntity s = socioRepository.findById(socioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

        // ✅ Traigo últimos pagos (TOP 10) usando TU repo (Pageable)
        var pageable = PageRequest.of(0, 10);
        List<PagoDto> ultimosPagos = pagoRepository
                .findBySocio_IdOrderByFechaPagoDesc(socioId, pageable)
                .stream()
                .map(p -> PagoDto.builder()
                        .id(p.getId())
                        .concepto(p.getConcepto() != null ? p.getConcepto().toString() : null)
                        .periodo(p.getPeriodo())
                        .montoTotal(p.getMontoTotal())
                        .medio(p.getMedio() != null ? p.getMedio().toString() : null)
                        .fechaPago(p.getFechaPago())
                        .mpPaymentId(p.getMpPaymentId())
                        .mpStatus(p.getMpStatus())
                        .build()
                )
                .toList();

        // ✅ Armamos el resumen (deuda por ahora null o un objeto vacío)
        SocioResumenDto dto = SocioResumenDto.builder()
                .socioId(s.getId())
                .dni(s.getDni())
                .nombre(s.getNombre())
                .apellido(s.getApellido())
                .disciplinaId(s.getDisciplina() != null ? s.getDisciplina().getId() : null)
                .disciplinaNombre(s.getDisciplina() != null ? s.getDisciplina().getNombre() : null)
                .activo(s.getActivo())
                .vigenciaHasta(s.getVigenciaHasta())
                .deuda(null) // ✅ después lo conectamos a tu DeudaResponseDto si querés
                .ultimosPagos(ultimosPagos)
                .build();

        return BaseResponse.ok("OK", dto);
    }

    @Override
    public BaseResponse<SocioDto> cambiarActivo(Long socioId, Boolean activo) {
        SocioEntity socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

        socio.setActivo(activo);
        SocioEntity saved = socioRepository.save(socio);

        return BaseResponse.ok(
                activo ? "Socio reactivado" : "Socio dado de baja",
                SocioMapper.toDto(saved)
        );
    }
}
