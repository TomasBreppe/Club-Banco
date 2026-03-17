package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.arancel.ArancelDisciplinaDto;
import com.example.demo.dto.deuda.DeudaItemDto;
import com.example.demo.dto.deuda.DeudaResponseDto;
import com.example.demo.dto.pagos.PagoDto;
import com.example.demo.dto.pagos.PagoManualRequestDto;
import com.example.demo.dto.pagos.PagoManualResponseDto;
import com.example.demo.dto.socio.SocioResumenDto;
import com.example.demo.entity.*;
import com.example.demo.repository.ArancelDisciplinaRepository;
import com.example.demo.repository.CuotaDisciplinaRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.SocioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.FinanzasService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.dto.arancel.ArancelCreateRequestDto;
import com.example.demo.repository.DisciplinaRepository;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanzasServiceImpl implements FinanzasService {

    private final SocioRepository socioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final CuotaDisciplinaRepository cuotaDisciplinaRepository;
    private final ArancelDisciplinaRepository arancelDisciplinaRepository;
    private final DisciplinaRepository disciplinaRepository;

    private static final DateTimeFormatter YYYY_MM = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<DeudaResponseDto> getDeudaBySocioId(Long socioId) {

        SocioEntity socio = socioRepository.findById(socioId).orElse(null);

        if (socio == null) {
            return new BaseResponse<>("Socio no encontrado", 404, null);
        }

        if (Boolean.FALSE.equals(socio.getActivo())) {
            return new BaseResponse<>("El socio está inactivo", 400, null);
        }

        if (socio.getDisciplina() == null) {
            return new BaseResponse<>("El socio no tiene disciplina asociada", 400, null);
        }

        if (socio.getArancelDisciplina() == null) {
            return new BaseResponse<>("El socio no tiene categoría/arancel asociado", 400, null);
        }

        Long disciplinaId = socio.getDisciplina().getId();
        String categoria = socio.getArancelDisciplina().getCategoria();

        ArancelDisciplinaEntity arancelActual = arancelDisciplinaRepository
                .findTopByDisciplina_IdAndCategoriaIgnoreCaseAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                        disciplinaId,
                        categoria,
                        LocalDate.now()
                )
                .orElse(null);

        if (arancelActual == null) {
            return new BaseResponse<>("No hay arancel vigente para la categoría del socio", 400, null);
        }

        ArancelDisciplinaEntity primerArancel = arancelDisciplinaRepository
                .findTopByDisciplina_IdAndCategoriaIgnoreCaseOrderByVigenteDesdeAsc(
                        disciplinaId,
                        categoria
                )
                .orElse(null);

        if (primerArancel == null) {
            return new BaseResponse<>("No hay historial de aranceles para la categoría del socio", 400, null);
        }

        // inscripción: si no existe pago INSCRIPCION, la debe
        boolean inscripcionPaga = Boolean.TRUE.equals(socio.getInscripcionPagada())
                || pagoRepository.existsBySocio_IdAndConcepto(socioId, "INSCRIPCION");

        YearMonth start = YearMonth.from(maxDate(
                primerArancel.getVigenteDesde(),
                socio.getCreatedAt().toLocalDate()
        ));

        YearMonth end = YearMonth.from(LocalDate.now());
        if (socio.getVigenciaHasta() != null) {
            YearMonth hasta = YearMonth.from(socio.getVigenciaHasta());
            if (hasta.isBefore(end)) end = hasta;
        }

        List<DeudaItemDto> items = new ArrayList<>();

        // si no pagó inscripción, agregarla como pendiente
        if (!inscripcionPaga) {
            items.add(DeudaItemDto.builder()
                    .periodo("INSCRIPCION")
                    .monto(BigDecimal.ZERO)
                    .pagado(false)
                    .build());
        }

        if (!end.isBefore(start)) {
            List<String> periodos = mesesEntre(start, end).stream()
                    .map(ym -> ym.format(YYYY_MM))
                    .toList();

            List<PagoEntity> pagos = pagoRepository.findBySocio_IdAndConceptoAndPeriodoIn(
                    socioId,
                    "CUOTA_MENSUAL",
                    periodos
            );

            Set<String> periodosPagos = pagos.stream()
                    .map(PagoEntity::getPeriodo)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<DeudaItemDto> cuotas = periodos.stream()
                    .map(p -> {
                        YearMonth ym = YearMonth.parse(p, YYYY_MM);
                        LocalDate fechaPeriodo = ym.atEndOfMonth();

                        ArancelDisciplinaEntity arancelPeriodo = arancelDisciplinaRepository
                                .findTopByDisciplina_IdAndCategoriaIgnoreCaseAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                                        disciplinaId,
                                        categoria,
                                        fechaPeriodo
                                )
                                .orElse(arancelActual);

                        BigDecimal montoPeriodo = calcularMontoArancel(arancelPeriodo);

                        return DeudaItemDto.builder()
                                .periodo(p)
                                .monto(montoPeriodo)
                                .pagado(periodosPagos.contains(p))
                                .build();
                    })
                    .toList();

            items.addAll(cuotas);
        }

        List<DeudaItemDto> adeudados = items.stream()
                .filter(i -> !i.isPagado())
                .toList();

        BigDecimal totalAdeudado = adeudados.stream()
                .map(DeudaItemDto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int mesesAdeudados = (int) adeudados.stream()
                .filter(i -> !"INSCRIPCION".equals(i.getPeriodo()))
                .count();

        DeudaResponseDto resp = DeudaResponseDto.builder()
                .socioId(socio.getId())
                .dni(socio.getDni())
                .nombreCompleto(socio.getNombre() + " " + socio.getApellido())
                .disciplina(socio.getDisciplina().getNombre())
                .vigenciaHasta(socio.getVigenciaHasta())
                .montoMensual(calcularMontoArancel(arancelActual))
                .mesesAdeudados(mesesAdeudados)
                .totalAdeudado(totalAdeudado)
                .items(items)
                .build();

        return new BaseResponse<>("Deuda calculada", 200, resp);
    }

    @Override
    @Transactional
    public BaseResponse<PagoManualResponseDto> registrarPagoManual(PagoManualRequestDto dto) {

        SocioEntity socio = socioRepository.findById(dto.getSocioId()).orElse(null);
        if (socio == null) return new BaseResponse<>("Socio no encontrado", 404, null);
        if (Boolean.FALSE.equals(socio.getActivo())) return new BaseResponse<>("El socio está inactivo", 400, null);

        String concepto = normalize(dto.getConcepto());
        String medioRaw = normalize(dto.getMedio());

        MedioPago medio;
        try {
            medio = MedioPago.valueOf(medioRaw);
        } catch (Exception ex) {
            return new BaseResponse<>("Medio de pago inválido", 400, null);
        }

        String periodo = dto.getPeriodo();

        if ("CUOTA_MENSUAL".equals(concepto)) {
            if (periodo == null || !periodo.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
                return new BaseResponse<>("Periodo inválido. Formato esperado: YYYY-MM", 400, null);
            }
            if (pagoRepository.existsBySocio_IdAndConceptoAndPeriodo(socio.getId(), "CUOTA_MENSUAL", periodo)) {
                return new BaseResponse<>("La cuota de ese periodo ya está paga", 400, null);
            }
        } else if ("INSCRIPCION".equals(concepto)) {
            boolean yaPagaInscripcion = Boolean.TRUE.equals(socio.getInscripcionPagada())
                    || pagoRepository.existsBySocio_IdAndConcepto(socio.getId(), "INSCRIPCION");

            if (yaPagaInscripcion) {
                return new BaseResponse<>("La inscripción ya fue pagada", 400, null);
            }
            periodo = null;
        } else {
            periodo = null;
        }

        DisciplinaEntity disciplina = socio.getDisciplina();
        ArancelDisciplinaEntity arancel = socio.getArancelDisciplina();

        if (dto.getArancelDisciplinaId() != null) {
            arancel = arancelDisciplinaRepository.findById(dto.getArancelDisciplinaId()).orElse(null);
            if (arancel == null) {
                return new BaseResponse<>("Arancel no encontrado", 404, null);
            }
            disciplina = arancel.getDisciplina();
        } else if (arancel != null) {
            disciplina = arancel.getDisciplina();
        } else if (dto.getDisciplinaId() != null && !Objects.equals(dto.getDisciplinaId(), disciplina.getId())) {
            return new BaseResponse<>("La disciplina enviada no coincide con la del socio", 400, null);
        }

        BigDecimal montoSocial;
        BigDecimal montoDisciplina;
        BigDecimal montoPreparacionFisica;
        BigDecimal montoTotal;
        String categoria;

        if (arancel != null) {
            montoSocial = safe(arancel.getMontoSocial());
            montoDisciplina = safe(arancel.getMontoDeportivo());
            montoPreparacionFisica = safe(arancel.getMontoPreparacionFisica());
            montoTotal = montoSocial.add(montoDisciplina).add(montoPreparacionFisica);
            categoria = arancel.getCategoria();
        } else {
            montoSocial = safe(dto.getMontoSocial());
            montoDisciplina = safe(dto.getMontoDisciplina());
            montoPreparacionFisica = safe(dto.getMontoPreparacionFisica());
            montoTotal = safe(dto.getMontoTotal());

            if (montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
                montoTotal = montoSocial.add(montoDisciplina).add(montoPreparacionFisica);
            }

            BigDecimal totalCalculado = montoSocial.add(montoDisciplina).add(montoPreparacionFisica);
            if (montoTotal.compareTo(totalCalculado) != 0) {
                return new BaseResponse<>("El monto total no coincide con la suma de social + disciplina + preparación física", 400, null);
            }

            categoria = dto.getCategoria();
        }

        if (montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return new BaseResponse<>("Monto total debe ser mayor a 0", 400, null);
        }

        PagoEntity pago = PagoEntity.builder()
                .socio(socio)
                .disciplina(disciplina)
                .arancelDisciplina(arancel)
                .concepto(concepto)
                .periodo(periodo)
                .categoria(categoria)
                .montoTotal(montoTotal)
                .montoSocial(montoSocial)
                .montoDisciplina(montoDisciplina)
                .montoPreparacionFisica(montoPreparacionFisica)
                .medio(medio)
                .observacion(dto.getObservacion())
                .fechaPago(LocalDateTime.now())
                .mpPaymentId(null)
                .mpStatus(null)
                .build();

        PagoEntity saved = pagoRepository.save(pago);

        if ("INSCRIPCION".equals(concepto)) {
            socio.setInscripcionPagada(true);
            socioRepository.save(socio);
        }

        if ("CUOTA_MENSUAL".equals(concepto) && periodo != null) {
            YearMonth ym = YearMonth.parse(periodo, YYYY_MM);
            LocalDate finMes = ym.atEndOfMonth();

            if (socio.getVigenciaHasta() == null || socio.getVigenciaHasta().isBefore(finMes)) {
                socio.setVigenciaHasta(finMes);
                socioRepository.save(socio);
            }
        }

        PagoManualResponseDto resp = PagoManualResponseDto.builder()
                .pagoId(saved.getId())
                .socioId(socio.getId())
                .disciplinaId(disciplina != null ? disciplina.getId() : null)
                .disciplinaNombre(disciplina != null ? disciplina.getNombre() : null)
                .arancelDisciplinaId(arancel != null ? arancel.getId() : null)
                .categoria(saved.getCategoria())
                .concepto(saved.getConcepto())
                .periodo(saved.getPeriodo())
                .montoTotal(saved.getMontoTotal())
                .montoSocial(saved.getMontoSocial())
                .montoDisciplina(saved.getMontoDisciplina())
                .montoPreparacionFisica(saved.getMontoPreparacionFisica())
                .medio(medio.name())
                .observacion(saved.getObservacion())
                .fechaPago(saved.getFechaPago())
                .build();

        return new BaseResponse<>("Pago registrado", 200, resp);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<PagoDto>> historialPagosSocio(Long socioId, LocalDate desde, LocalDate hasta) {

        SocioEntity socio = socioRepository.findById(socioId).orElse(null);
        if (socio == null) return new BaseResponse<>("Socio no encontrado", 404, null);

        var pagos = buscarPagosPorRango(socioId, desde, hasta);
        var data = pagos.stream().map(this::toPagoDto).toList();
        return new BaseResponse<>("OK", 200, data);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<PagoDto>> misPagos(String email, LocalDate desde, LocalDate hasta) {

        UsuarioEntity user = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) return new BaseResponse<>("Usuario no encontrado", 404, null);

        if (user.getSocios() == null || user.getSocios().isEmpty()) {
            return new BaseResponse<>("El usuario no tiene socio asociado", 400, null);
        }

        if (user.getSocios().size() != 1) {
            return new BaseResponse<>("El usuario tiene múltiples socios asociados. Elegí uno.", 400, null);
        }

        Long socioId = user.getSocios().iterator().next().getId();

        var pagos = buscarPagosPorRango(socioId, desde, hasta);
        var data = pagos.stream().map(this::toPagoDto).toList();
        return new BaseResponse<>("OK", 200, data);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<SocioResumenDto> resumenSocio(Long socioId, LocalDate desde, LocalDate hasta, Integer limit) {

        SocioEntity socio = socioRepository.findById(socioId).orElse(null);
        if (socio == null) return BaseResponse.bad("Socio no encontrado");

        var deudaResp = getDeudaBySocioId(socioId);
        var deuda = deudaResp.getData();

        int lim = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);

        List<PagoEntity> pagos;
        if (desde != null || hasta != null) {
            pagos = buscarPagosPorRango(socioId, desde, hasta);
            if (pagos.size() > lim) pagos = pagos.subList(0, lim);
        } else {
            pagos = pagoRepository.findBySocio_IdOrderByFechaPagoDesc(
                    socioId,
                    PageRequest.of(0, lim)
            );
        }

        var ultimos = pagos.stream().map(this::toPagoDto).toList();

        SocioResumenDto resumen = SocioResumenDto.builder()
                .socioId(socio.getId())
                .dni(socio.getDni())
                .nombre(socio.getNombre())
                .apellido(socio.getApellido())
                .activo(socio.getActivo())
                .vigenciaHasta(socio.getVigenciaHasta())
                .disciplinaId(socio.getDisciplina() != null ? socio.getDisciplina().getId() : null)
                .disciplinaNombre(socio.getDisciplina() != null ? socio.getDisciplina().getNombre() : null)
                .arancelDisciplinaId(socio.getArancelDisciplina() != null ? socio.getArancelDisciplina().getId() : null)
                .categoriaArancel(socio.getArancelDisciplina() != null ? socio.getArancelDisciplina().getCategoria() : null)
                .deuda(deuda)
                .ultimosPagos(ultimos)
                .build();

        return BaseResponse.ok("Resumen OK", resumen);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<ArancelDisciplinaDto>> listarArancelesActivos() {
        List<ArancelDisciplinaDto> data = arancelDisciplinaRepository.findByActivaTrueOrderByDisciplina_IdAscCategoriaAsc()
                .stream()
                .map(this::toArancelDto)
                .toList();

        return BaseResponse.ok("Aranceles OK", data);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<ArancelDisciplinaDto>> listarArancelesPorDisciplina(Long disciplinaId) {
        List<ArancelDisciplinaDto> data = arancelDisciplinaRepository.findByDisciplina_IdAndActivaTrueOrderByCategoriaAsc(disciplinaId)
                .stream()
                .map(this::toArancelDto)
                .toList();

        return BaseResponse.ok("Aranceles por disciplina OK", data);
    }

    @Override
    @Transactional
    public BaseResponse<ArancelDisciplinaDto> crearArancel(ArancelCreateRequestDto dto) {
        if (dto.getDisciplinaId() == null) {
            return BaseResponse.bad("La disciplina es obligatoria");
        }

        DisciplinaEntity disciplina = disciplinaRepository.findById(dto.getDisciplinaId()).orElse(null);
        if (disciplina == null) {
            return BaseResponse.bad("Disciplina no encontrada");
        }

        if (Boolean.FALSE.equals(disciplina.getActiva())) {
            return BaseResponse.bad("La disciplina está inactiva");
        }

        String categoria = dto.getCategoria() != null ? dto.getCategoria().trim() : "";
        if (categoria.isBlank()) {
            return BaseResponse.bad("La categoría es obligatoria");
        }

        BigDecimal montoSocial = safe(dto.getMontoSocial());
        BigDecimal montoDeportivo = safe(dto.getMontoDeportivo());
        BigDecimal montoPreparacionFisica = safe(dto.getMontoPreparacionFisica());

        if (montoSocial.compareTo(BigDecimal.ZERO) < 0 ||
                montoDeportivo.compareTo(BigDecimal.ZERO) < 0 ||
                montoPreparacionFisica.compareTo(BigDecimal.ZERO) < 0) {
            return BaseResponse.bad("Los montos no pueden ser negativos");
        }

        if (montoSocial.add(montoDeportivo).add(montoPreparacionFisica).compareTo(BigDecimal.ZERO) <= 0) {
            return BaseResponse.bad("La suma de los montos debe ser mayor a 0");
        }

        LocalDate vigenteDesde = dto.getVigenteDesde() != null ? dto.getVigenteDesde() : LocalDate.now();

        ArancelDisciplinaEntity entity = ArancelDisciplinaEntity.builder()
                .disciplina(disciplina)
                .categoria(categoria.toUpperCase(Locale.ROOT))
                .montoSocial(montoSocial)
                .montoDeportivo(montoDeportivo)
                .montoPreparacionFisica(montoPreparacionFisica)
                .vigenteDesde(vigenteDesde)
                .activa(true)
                .build();

        ArancelDisciplinaEntity saved = arancelDisciplinaRepository.save(entity);

        return BaseResponse.ok("Arancel creado correctamente", toArancelDto(saved));
    }

    @Override
    @Transactional
    public BaseResponse<ArancelDisciplinaDto> cambiarEstadoArancel(Long arancelId, boolean activa) {
        ArancelDisciplinaEntity entity = arancelDisciplinaRepository.findById(arancelId).orElse(null);
        if (entity == null) {
            return BaseResponse.bad("Arancel no encontrado");
        }

        entity.setActiva(activa);
        ArancelDisciplinaEntity saved = arancelDisciplinaRepository.save(entity);

        return BaseResponse.ok(
                activa ? "Arancel activado correctamente" : "Arancel desactivado correctamente",
                toArancelDto(saved)
        );
    }

    private List<PagoEntity> buscarPagosPorRango(Long socioId, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return pagoRepository.findBySocio_IdOrderByFechaPagoDesc(socioId);
        }

        if (desde != null && hasta == null) {
            LocalDateTime d = desde.atStartOfDay();
            LocalDateTime h = LocalDate.now().atTime(23, 59, 59);
            return pagoRepository.findBySocio_IdAndFechaPagoBetweenOrderByFechaPagoDesc(socioId, d, h);
        }

        if (desde == null) {
            LocalDateTime d = LocalDate.of(2000, 1, 1).atStartOfDay();
            LocalDateTime h = hasta.atTime(23, 59, 59);
            return pagoRepository.findBySocio_IdAndFechaPagoBetweenOrderByFechaPagoDesc(socioId, d, h);
        }

        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("hasta no puede ser menor que desde");
        }

        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hasta.atTime(23, 59, 59);
        return pagoRepository.findBySocio_IdAndFechaPagoBetweenOrderByFechaPagoDesc(socioId, d, h);
    }

    private PagoDto toPagoDto(PagoEntity p) {
        return PagoDto.builder()
                .id(p.getId())
                .concepto(p.getConcepto())
                .periodo(p.getPeriodo())
                .disciplinaId(p.getDisciplina() != null ? p.getDisciplina().getId() : null)
                .disciplinaNombre(p.getDisciplina() != null ? p.getDisciplina().getNombre() : null)
                .categoria(p.getCategoria())
                .montoTotal(p.getMontoTotal())
                .montoSocial(p.getMontoSocial())
                .montoDisciplina(p.getMontoDisciplina())
                .montoPreparacionFisica(p.getMontoPreparacionFisica())
                .medio(p.getMedio() != null ? p.getMedio().name() : null)
                .observacion(p.getObservacion())
                .fechaPago(p.getFechaPago())
                .mpPaymentId(p.getMpPaymentId())
                .mpStatus(p.getMpStatus())
                .build();
    }

    private ArancelDisciplinaDto toArancelDto(ArancelDisciplinaEntity a) {
        BigDecimal total = safe(a.getMontoSocial())
                .add(safe(a.getMontoDeportivo()))
                .add(safe(a.getMontoPreparacionFisica()));

        return ArancelDisciplinaDto.builder()
                .id(a.getId())
                .disciplinaId(a.getDisciplina().getId())
                .disciplinaNombre(a.getDisciplina().getNombre())
                .categoria(a.getCategoria())
                .montoSocial(safe(a.getMontoSocial()))
                .montoDeportivo(safe(a.getMontoDeportivo()))
                .montoPreparacionFisica(safe(a.getMontoPreparacionFisica()))
                .montoTotal(total)
                .vigenteDesde(a.getVigenteDesde())
                .activa(a.getActiva())
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal calcularMontoArancel(ArancelDisciplinaEntity a) {
        if (a == null) return BigDecimal.ZERO;

        return safe(a.getMontoSocial())
                .add(safe(a.getMontoDeportivo()))
                .add(safe(a.getMontoPreparacionFisica()));
    }

    private static List<YearMonth> mesesEntre(YearMonth start, YearMonth end) {
        List<YearMonth> out = new ArrayList<>();
        YearMonth curr = start;
        while (!curr.isAfter(end)) {
            out.add(curr);
            curr = curr.plusMonths(1);
        }
        return out;
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    private static DeudaResponseDto baseDeudaResponse(SocioEntity socio, BigDecimal montoMensual, List<DeudaItemDto> items) {
        return DeudaResponseDto.builder()
                .socioId(socio.getId())
                .dni(socio.getDni())
                .nombreCompleto(socio.getNombre() + " " + socio.getApellido())
                .disciplina(socio.getDisciplina().getNombre())
                .vigenciaHasta(socio.getVigenciaHasta())
                .montoMensual(montoMensual)
                .mesesAdeudados(0)
                .totalAdeudado(BigDecimal.ZERO)
                .items(items)
                .build();
    }
}