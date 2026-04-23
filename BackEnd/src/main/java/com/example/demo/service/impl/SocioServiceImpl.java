package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.pagos.PagoDto;
import com.example.demo.dto.socio.*;
import com.example.demo.entity.ArancelDisciplinaEntity;
import com.example.demo.entity.DisciplinaEntity;
import com.example.demo.entity.Genero;
import com.example.demo.entity.SocioDisciplinaEntity;
import com.example.demo.entity.SocioEntity;
import com.example.demo.mapper.SocioMapper;
import com.example.demo.repository.ArancelDisciplinaRepository;
import com.example.demo.repository.DisciplinaRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.SocioDisciplinaRepository;
import com.example.demo.repository.SocioRepository;
import com.example.demo.service.FinanzasService;
import com.example.demo.service.SocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocioServiceImpl implements SocioService {

        private final SocioRepository socioRepository;
        private final DisciplinaRepository disciplinaRepository;
        private final PagoRepository pagoRepository;
        private final ArancelDisciplinaRepository arancelDisciplinaRepository;
        private final SocioDisciplinaRepository socioDisciplinaRepository;
        private final FinanzasService finanzasService;

        @Override
        @Transactional
        public BaseResponse<SocioDto> crear(SocioCreateDto dto) {

                if (socioRepository.findByDni(dto.getDni().trim()).isPresent()) {
                        return BaseResponse.bad("Ya existe un socio con ese DNI");
                }

                if (dto.getDisciplinaId() == null) {
                        return BaseResponse.bad("Debés seleccionar una disciplina");
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
                                .activo(true)
                                .tieneBeca(dto.getTieneBeca() != null ? dto.getTieneBeca() : false)
                                .porcentajeBecaSocial(
                                                dto.getPorcentajeBecaSocial() != null ? dto.getPorcentajeBecaSocial()
                                                                : BigDecimal.ZERO)
                                .porcentajeBecaDeportiva(dto.getPorcentajeBecaDeportiva() != null
                                                ? dto.getPorcentajeBecaDeportiva()
                                                : BigDecimal.ZERO)
                                .porcentajeBecaPreparacionFisica(dto.getPorcentajeBecaPreparacionFisica() != null
                                                ? dto.getPorcentajeBecaPreparacionFisica()
                                                : BigDecimal.ZERO)
                                .observacionBeca(dto.getObservacionBeca() != null ? dto.getObservacionBeca().trim()
                                                : null)
                                .build();

                SocioEntity saved = socioRepository.save(socio);

                SocioDisciplinaEntity socioDisciplina = SocioDisciplinaEntity.builder()
                                .socio(saved)
                                .disciplina(disc)
                                .arancelDisciplina(arancel)
                                .activo(true)
                                .vigenciaHasta(null)
                                .inscripcionPagada(
                                                dto.getInscripcionPagada() != null ? dto.getInscripcionPagada() : false)
                                .build();

                socioDisciplinaRepository.save(socioDisciplina);

                return BaseResponse.created("Socio creado", SocioMapper.toDto(saved, socioDisciplina));
        }

        @Override
        public BaseResponse<List<SocioDto>> listar(Long disciplinaId, String categoria, String estadoPago, String q) {

                String qTexto = (q == null || q.isBlank()) ? null : String.valueOf(q).trim();
                String categoriaNorm = (categoria == null || categoria.isBlank()) ? null
                                : categoria.trim().toUpperCase();
                String ep = (estadoPago == null || estadoPago.isBlank()) ? null : estadoPago.trim().toUpperCase();

                LocalDate hoy = LocalDate.now();

                List<SocioEntity> socios = socioRepository.search(
                                disciplinaId,
                                categoriaNorm,
                                qTexto);

                List<Long> socioIds = socios.stream().map(SocioEntity::getId).toList();

                List<SocioDisciplinaEntity> relaciones = socioIds.isEmpty()
                                ? List.of()
                                : socioDisciplinaRepository
                                                .findBySocio_IdInAndActivoTrueOrderBySocio_IdAscIdAsc(socioIds);

                Map<Long, SocioDisciplinaEntity> primeraRelacionPorSocio = new LinkedHashMap<>();
                Map<Long, Boolean> deudaPorSocio = new LinkedHashMap<>();

                Map<Long, Long> principalIdPorSocio = new LinkedHashMap<>();

                for (SocioDisciplinaEntity sd : relaciones) {
                        Long socioId = sd.getSocio().getId();
                        primeraRelacionPorSocio.putIfAbsent(socioId, sd);
                        principalIdPorSocio.putIfAbsent(socioId, sd.getId());
                }

                for (SocioDisciplinaEntity sd : relaciones) {
                        Long socioId = sd.getSocio().getId();
                        boolean esPrincipal = java.util.Objects.equals(principalIdPorSocio.get(socioId), sd.getId());

                        boolean debeInscripcion = esPrincipal && Boolean.FALSE.equals(sd.getInscripcionPagada());
                        boolean debeCuota = sd.getVigenciaHasta() == null || sd.getVigenciaHasta().isBefore(hoy);

                        deudaPorSocio.merge(socioId, (debeInscripcion || debeCuota), Boolean::logicalOr);
                }

                List<SocioDto> result = socios.stream()
                                .map(s -> {
                                        SocioDisciplinaEntity sd = primeraRelacionPorSocio.get(s.getId());
                                        SocioDto dto = SocioMapper.toDto(s, sd);

                                        if (Boolean.FALSE.equals(s.getActivo())) {
                                                dto.setEstadoPago("INACTIVO");
                                                return dto;
                                        }

                                        boolean tieneDeuda = deudaPorSocio.getOrDefault(s.getId(), false);
                                        dto.setEstadoPago(tieneDeuda ? "DEBE" : "AL_DIA");
                                        return dto;
                                })
                                .filter(dto -> ep == null || ep.equals(dto.getEstadoPago()))
                                .toList();

                return BaseResponse.ok("OK", result);
        }

        @Override
        public BaseResponse<SocioResumenDto> resumen(Long socioId) {
                return finanzasService.resumenSocio(socioId, null, null, 10);
        }

        @Override
        @Transactional
        public BaseResponse<SocioDto> cambiarActivo(Long socioId, Boolean activo) {
                SocioEntity socio = socioRepository.findById(socioId)
                                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

                socio.setActivo(activo);
                SocioEntity saved = socioRepository.save(socio);

                List<SocioDisciplinaEntity> relaciones = socioDisciplinaRepository.findBySocio_IdOrderByIdAsc(socioId);
                relaciones.forEach(sd -> sd.setActivo(activo));
                socioDisciplinaRepository.saveAll(relaciones);

                SocioDisciplinaEntity sd = relaciones.stream().findFirst().orElse(null);

                return BaseResponse.ok(
                                activo ? "Socio reactivado" : "Socio dado de baja",
                                SocioMapper.toDto(saved, sd));
        }

        @Override
        @Transactional
        public BaseResponse<SocioDto> agregarDisciplina(Long socioId, SocioAgregarDisciplinaDto dto) {

                SocioEntity socio = socioRepository.findById(socioId).orElse(null);
                if (socio == null) {
                        return BaseResponse.bad("Socio no encontrado");
                }

                if (Boolean.FALSE.equals(socio.getActivo())) {
                        return BaseResponse.bad("El socio está inactivo");
                }

                DisciplinaEntity disciplina = disciplinaRepository.findById(dto.getDisciplinaId()).orElse(null);
                if (disciplina == null) {
                        return BaseResponse.bad("Disciplina no encontrada");
                }

                ArancelDisciplinaEntity arancel = arancelDisciplinaRepository.findById(dto.getArancelDisciplinaId())
                                .orElse(null);
                if (arancel == null) {
                        return BaseResponse.bad("Arancel no encontrado");
                }

                if (!arancel.getDisciplina().getId().equals(disciplina.getId())) {
                        return BaseResponse.bad("El arancel no corresponde a la disciplina");
                }

                if (Boolean.FALSE.equals(arancel.getActiva())) {
                        return BaseResponse.bad("El arancel está inactivo");
                }

                boolean yaTiene = socioDisciplinaRepository
                                .findBySocio_IdAndDisciplina_IdAndActivoTrue(socioId, dto.getDisciplinaId())
                                .isPresent();

                if (yaTiene) {
                        return BaseResponse.bad("El socio ya tiene esa disciplina activa");
                }

                SocioDisciplinaEntity nueva = SocioDisciplinaEntity.builder()
                                .socio(socio)
                                .disciplina(disciplina)
                                .arancelDisciplina(arancel)
                                .activo(true)
                                .vigenciaHasta(null)
                                .inscripcionPagada(
                                                dto.getInscripcionPagada() != null ? dto.getInscripcionPagada() : false)
                                .build();

                socioDisciplinaRepository.save(nueva);

                return BaseResponse.ok("Disciplina agregada correctamente", SocioMapper.toDto(socio, nueva));
        }

        @Override
        @Transactional
        public BaseResponse<SocioDto> actualizarBeca(Long socioId, SocioBecaUpdateDto dto) {
                SocioEntity socio = socioRepository.findById(socioId)
                                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

                socio.setTieneBeca(dto.getTieneBeca() != null ? dto.getTieneBeca() : false);
                socio.setPorcentajeBecaSocial(normalizarPorcentaje(dto.getPorcentajeBecaSocial()));
                socio.setPorcentajeBecaDeportiva(normalizarPorcentaje(dto.getPorcentajeBecaDeportiva()));
                socio.setPorcentajeBecaPreparacionFisica(
                                normalizarPorcentaje(dto.getPorcentajeBecaPreparacionFisica()));
                socio.setObservacionBeca(dto.getObservacionBeca() != null ? dto.getObservacionBeca().trim() : null);

                if (!Boolean.TRUE.equals(socio.getTieneBeca())) {
                        socio.setPorcentajeBecaSocial(BigDecimal.ZERO);
                        socio.setPorcentajeBecaDeportiva(BigDecimal.ZERO);
                        socio.setPorcentajeBecaPreparacionFisica(BigDecimal.ZERO);
                        socio.setObservacionBeca(null);
                }

                SocioEntity saved = socioRepository.save(socio);

                SocioDisciplinaEntity sd = socioDisciplinaRepository
                                .findFirstBySocio_IdAndActivoTrueOrderByIdAsc(saved.getId())
                                .orElse(null);

                return BaseResponse.ok("Beca actualizada correctamente", SocioMapper.toDto(saved, sd));
        }

        @Override
        @Transactional
        public BaseResponse<SocioDto> cambiarCategoriaDisciplina(Long socioDisciplinaId, SocioCambiarCategoriaDto dto) {
                if (dto == null || dto.getArancelDisciplinaId() == null || dto.getArancelDisciplinaId() <= 0) {
                        return BaseResponse.bad("Debés seleccionar una categoría/arancel");
                }

                SocioDisciplinaEntity sd = socioDisciplinaRepository.findByIdAndActivoTrue(socioDisciplinaId)
                                .orElse(null);

                if (sd == null) {
                        return BaseResponse.bad("No se encontró la disciplina activa del socio");
                }

                ArancelDisciplinaEntity nuevoArancel = arancelDisciplinaRepository
                                .findById(dto.getArancelDisciplinaId())
                                .orElse(null);

                if (nuevoArancel == null) {
                        return BaseResponse.bad("Arancel no encontrado");
                }

                if (Boolean.FALSE.equals(nuevoArancel.getActiva())) {
                        return BaseResponse.bad("El arancel seleccionado está inactivo");
                }

                if (sd.getDisciplina() == null || nuevoArancel.getDisciplina() == null ||
                                !sd.getDisciplina().getId().equals(nuevoArancel.getDisciplina().getId())) {
                        return BaseResponse.bad("La categoría seleccionada no corresponde a la disciplina");
                }

                sd.setArancelDisciplina(nuevoArancel);
                SocioDisciplinaEntity savedSd = socioDisciplinaRepository.save(sd);

                SocioEntity socio = savedSd.getSocio();

                return BaseResponse.ok(
                                "Categoría actualizada correctamente",
                                SocioMapper.toDto(socio, savedSd));
        }

        private BigDecimal normalizarPorcentaje(BigDecimal value) {
                if (value == null)
                        return BigDecimal.ZERO;
                if (value.compareTo(BigDecimal.ZERO) < 0)
                        return BigDecimal.ZERO;
                if (value.compareTo(BigDecimal.valueOf(100)) > 0)
                        return BigDecimal.valueOf(100);
                return value;
        }
}