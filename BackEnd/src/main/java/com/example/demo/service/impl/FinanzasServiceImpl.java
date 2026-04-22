package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.arancel.ArancelCreateRequestDto;
import com.example.demo.dto.arancel.ArancelDisciplinaDto;
import com.example.demo.dto.deuda.DeudaItemDto;
import com.example.demo.dto.deuda.DeudaResponseDto;
import com.example.demo.dto.pagos.PagoDto;
import com.example.demo.dto.pagos.PagoManualRequestDto;
import com.example.demo.dto.pagos.PagoManualResponseDto;
import com.example.demo.dto.socio.SocioDisciplinaResumenDto;
import com.example.demo.dto.socio.SocioResumenDto;
import com.example.demo.entity.*;
import com.example.demo.repository.ArancelDisciplinaRepository;
import com.example.demo.repository.CuotaDisciplinaRepository;
import com.example.demo.repository.DisciplinaRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.SocioDisciplinaRepository;
import com.example.demo.repository.SocioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.FinanzasService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    private final SocioDisciplinaRepository socioDisciplinaRepository;

    private static final YearMonth PERIODO_INICIO_SISTEMA = YearMonth.of(2026, 4);
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

        SocioDisciplinaEntity socioDisciplina = socioDisciplinaRepository
                .findFirstBySocio_IdAndActivoTrueOrderByIdAsc(socioId)
                .orElse(null);

        if (socioDisciplina == null) {
            return new BaseResponse<>("El socio no tiene disciplina activa asociada", 400, null);
        }

        return new BaseResponse<>("Deuda calculada", 200, calcularDeudaPorDisciplina(socio, socioDisciplina));
    }

    private DeudaResponseDto calcularDeudaPorDisciplina(SocioEntity socio, SocioDisciplinaEntity socioDisciplina) {

        if (socioDisciplina.getDisciplina() == null) {
            throw new IllegalArgumentException("La relación socio-disciplina no tiene disciplina asociada");
        }

        if (socioDisciplina.getArancelDisciplina() == null) {
            throw new IllegalArgumentException("La relación socio-disciplina no tiene categoría/arancel asociado");
        }

        List<SocioDisciplinaEntity> relacionesActivas = socioDisciplinaRepository
                .findBySocio_IdAndActivoTrue(socio.getId());

        Long principalId = relacionesActivas.isEmpty() ? null : relacionesActivas.get(0).getId();
        boolean esPrimeraDisciplina = Objects.equals(socioDisciplina.getId(), principalId);

        Long disciplinaId = socioDisciplina.getDisciplina().getId();
        String categoria = socioDisciplina.getArancelDisciplina().getCategoria();

        ArancelDisciplinaEntity arancelActual = arancelDisciplinaRepository
                .findTopByDisciplina_IdAndCategoriaIgnoreCaseAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                        disciplinaId,
                        categoria,
                        LocalDate.now())
                .orElse(null);

        if (arancelActual == null) {
            throw new IllegalArgumentException("No hay arancel vigente para la categoría de la disciplina");
        }

        ArancelDisciplinaEntity primerArancel = arancelDisciplinaRepository
                .findTopByDisciplina_IdAndCategoriaIgnoreCaseOrderByVigenteDesdeAsc(
                        disciplinaId,
                        categoria)
                .orElse(null);

        if (primerArancel == null) {
            throw new IllegalArgumentException("No hay historial de aranceles para la categoría de la disciplina");
        }

        boolean inscripcionPaga;

        if (esPrimeraDisciplina) {
            inscripcionPaga = Boolean.TRUE.equals(socioDisciplina.getInscripcionPagada())
                    || pagoRepository.existsBySocioDisciplina_IdAndConceptoAndAnuladoFalse(
                            socioDisciplina.getId(),
                            "INSCRIPCION");
        } else {
            inscripcionPaga = true;
        }

        YearMonth start = Collections.max(List.of(
                PERIODO_INICIO_SISTEMA,
                YearMonth.from(primerArancel.getVigenteDesde()),
                YearMonth.from(socioDisciplina.getFechaAlta().toLocalDate())));

        YearMonth end = YearMonth.from(LocalDate.now());

        List<DeudaItemDto> items = new ArrayList<>();

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

            List<PagoEntity> pagos = pagoRepository
                    .findBySocioDisciplina_IdAndConceptoAndPeriodoIn(
                            socioDisciplina.getId(),
                            "CUOTA_MENSUAL",
                            periodos)
                    .stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getAnulado()))
                    .toList();

            Map<String, BigDecimal> pagadoPorPeriodo = pagos.stream()
                    .filter(p -> p.getPeriodo() != null)
                    .collect(Collectors.groupingBy(
                            PagoEntity::getPeriodo,
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    p -> safe(p.getMontoTotal()),
                                    BigDecimal::add)));

            List<DeudaItemDto> cuotas = periodos.stream()
                    .map(p -> {
                        YearMonth ym = YearMonth.parse(p, YYYY_MM);
                        LocalDate fechaPeriodo = ym.atEndOfMonth();

                        ArancelDisciplinaEntity arancelPeriodo = arancelDisciplinaRepository
                                .findTopByDisciplina_IdAndCategoriaIgnoreCaseAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                                        disciplinaId,
                                        categoria,
                                        fechaPeriodo)
                                .orElse(arancelActual);

                        BigDecimal montoPeriodo = calcularMontoArancelConBeca(arancelPeriodo, socio,
                                esPrimeraDisciplina);
                        BigDecimal pagado = safe(pagadoPorPeriodo.get(p));

                        boolean cuotaCompleta = pagado.compareTo(montoPeriodo) >= 0;

                        BigDecimal saldoPendiente = montoPeriodo.subtract(pagado);
                        if (saldoPendiente.compareTo(BigDecimal.ZERO) < 0) {
                            saldoPendiente = BigDecimal.ZERO;
                        }

                        return DeudaItemDto.builder()
                                .periodo(p)
                                .monto(saldoPendiente)
                                .pagado(cuotaCompleta)
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

        return DeudaResponseDto.builder()
                .socioId(socio.getId())
                .dni(socio.getDni())
                .nombreCompleto(socio.getNombre() + " " + socio.getApellido())
                .disciplina(socioDisciplina.getDisciplina().getNombre())
                .vigenciaHasta(socioDisciplina.getVigenciaHasta())
                .montoMensual(calcularMontoArancelConBeca(arancelActual, socio, esPrimeraDisciplina))
                .mesesAdeudados(mesesAdeudados)
                .totalAdeudado(totalAdeudado)
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public BaseResponse<PagoManualResponseDto> registrarPagoManual(PagoManualRequestDto dto) {

        SocioEntity socio = socioRepository.findById(dto.getSocioId()).orElse(null);
        if (socio == null) {
            return new BaseResponse<>("Socio no encontrado", 404, null);
        }
        if (Boolean.FALSE.equals(socio.getActivo())) {
            return new BaseResponse<>("El socio está inactivo", 400, null);
        }

        String concepto = normalize(dto.getConcepto());
        String medioRaw = normalize(dto.getMedio());

        MedioPago medio;
        try {
            medio = MedioPago.valueOf(medioRaw);
        } catch (Exception ex) {
            return new BaseResponse<>("Medio de pago inválido", 400, null);
        }

        SocioDisciplinaEntity socioDisciplina;

        if (dto.getDisciplinaId() != null) {
            socioDisciplina = socioDisciplinaRepository
                    .findBySocio_IdAndDisciplina_IdAndActivoTrue(dto.getSocioId(), dto.getDisciplinaId())
                    .orElse(null);
        } else {
            socioDisciplina = socioDisciplinaRepository
                    .findFirstBySocio_IdAndActivoTrueOrderByIdAsc(dto.getSocioId())
                    .orElse(null);
        }

        if (socioDisciplina == null) {
            return new BaseResponse<>("No se encontró una disciplina activa para el socio", 400, null);
        }

        String periodo = dto.getPeriodo();

        if ("CUOTA_MENSUAL".equals(concepto)) {
            if (periodo == null || !periodo.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
                return new BaseResponse<>("Periodo inválido. Formato esperado: YYYY-MM", 400, null);
            }

            YearMonth periodoYm = YearMonth.parse(periodo, YYYY_MM);

            if (periodoYm.isBefore(PERIODO_INICIO_SISTEMA)) {
                return new BaseResponse<>(
                        "Solo se permiten cuotas mensuales desde 2026-04. Las anteriores deben registrarse como ingreso manual.",
                        400,
                        null);
            }

        } else if ("INSCRIPCION".equals(concepto)) {

            List<SocioDisciplinaEntity> relacionesActivas = socioDisciplinaRepository
                    .findBySocio_IdAndActivoTrue(dto.getSocioId());

            Long principalId = relacionesActivas.isEmpty() ? null : relacionesActivas.get(0).getId();
            boolean esPrimeraDisciplina = Objects.equals(socioDisciplina.getId(), principalId);

            if (!esPrimeraDisciplina) {
                return new BaseResponse<>("La inscripción solo se cobra en la disciplina principal", 400, null);
            }

            boolean yaPagaInscripcion = Boolean.TRUE.equals(socioDisciplina.getInscripcionPagada())
                    || pagoRepository.existsBySocioDisciplina_IdAndConceptoAndAnuladoFalse(
                            socioDisciplina.getId(),
                            "INSCRIPCION");

            if (yaPagaInscripcion) {
                return new BaseResponse<>("La inscripción ya fue pagada para esa disciplina", 400, null);
            }

            periodo = null;

        } else {
            return new BaseResponse<>("Concepto inválido", 400, null);
        }

        DisciplinaEntity disciplina = socioDisciplina.getDisciplina();
        ArancelDisciplinaEntity arancel = null;

        BigDecimal montoSocial = BigDecimal.ZERO;
        BigDecimal montoDisciplina = BigDecimal.ZERO;
        BigDecimal montoPreparacionFisica = BigDecimal.ZERO;
        BigDecimal montoTotal;
        String categoria = null;

        if ("CUOTA_MENSUAL".equals(concepto)) {
            arancel = socioDisciplina.getArancelDisciplina();

            if (dto.getArancelDisciplinaId() != null) {
                arancel = arancelDisciplinaRepository.findById(dto.getArancelDisciplinaId()).orElse(null);
                if (arancel == null) {
                    return new BaseResponse<>("Arancel no encontrado", 404, null);
                }
            }

            if (arancel == null) {
                return new BaseResponse<>("La disciplina del socio no tiene arancel asociado", 400, null);
            }

            List<SocioDisciplinaEntity> relacionesActivas = socioDisciplinaRepository
                    .findBySocio_IdAndActivoTrue(dto.getSocioId());

            Long principalId = relacionesActivas.isEmpty() ? null : relacionesActivas.get(0).getId();
            boolean esPrimeraDisciplina = Objects.equals(socioDisciplina.getId(), principalId);

            disciplina = arancel.getDisciplina();

            BigDecimal esperadoSocial;
            BigDecimal esperadoDisciplina;
            BigDecimal esperadoPreparacion;

            if (esPrimeraDisciplina) {
                esperadoSocial = aplicarBeca(arancel.getMontoSocial(), socio.getPorcentajeBecaSocial());
                esperadoDisciplina = aplicarBeca(arancel.getMontoDeportivo(), socio.getPorcentajeBecaDeportiva());
                esperadoPreparacion = aplicarBeca(
                        arancel.getMontoPreparacionFisica(),
                        socio.getPorcentajeBecaPreparacionFisica());
            } else {
                esperadoSocial = BigDecimal.ZERO;
                esperadoDisciplina = aplicarBeca(arancel.getMontoDeportivo(), socio.getPorcentajeBecaDeportiva());
                esperadoPreparacion = aplicarBeca(
                        arancel.getMontoPreparacionFisica(),
                        socio.getPorcentajeBecaPreparacionFisica());
            }

            BigDecimal montoEsperado = esperadoSocial.add(esperadoDisciplina).add(esperadoPreparacion);

            BigDecimal yaPagado = safe(
                    pagoRepository.sumarMontoPagadoPorPeriodo(
                            socioDisciplina.getId(),
                            "CUOTA_MENSUAL",
                            periodo));

            BigDecimal saldoPendiente = montoEsperado.subtract(yaPagado);
            if (saldoPendiente.compareTo(BigDecimal.ZERO) < 0) {
                saldoPendiente = BigDecimal.ZERO;
            }

            BigDecimal montoIngresado = safe(dto.getMontoTotal());

            if (montoIngresado.compareTo(BigDecimal.ZERO) <= 0) {
                return new BaseResponse<>("El monto a pagar debe ser mayor a 0", 400, null);
            }

            if (saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
                return new BaseResponse<>("La cuota de ese período ya está completamente pagada", 400, null);
            }

            if (montoIngresado.compareTo(saldoPendiente) > 0) {
                return new BaseResponse<>("El monto ingresado supera el saldo pendiente de la cuota", 400, null);
            }

            montoTotal = montoIngresado;
            categoria = arancel.getCategoria();

            if (montoEsperado.compareTo(BigDecimal.ZERO) > 0) {
                montoSocial = esperadoSocial.multiply(montoIngresado)
                        .divide(montoEsperado, 2, java.math.RoundingMode.HALF_UP);

                montoDisciplina = esperadoDisciplina.multiply(montoIngresado)
                        .divide(montoEsperado, 2, java.math.RoundingMode.HALF_UP);

                montoPreparacionFisica = montoIngresado.subtract(montoSocial).subtract(montoDisciplina);
            } else {
                montoSocial = BigDecimal.ZERO;
                montoDisciplina = BigDecimal.ZERO;
                montoPreparacionFisica = BigDecimal.ZERO;
            }

        } else {
            montoTotal = safe(dto.getMontoTotal());

            if (montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
                return new BaseResponse<>("Monto total debe ser mayor a 0", 400, null);
            }

            montoSocial = BigDecimal.ZERO;
            montoDisciplina = BigDecimal.ZERO;
            montoPreparacionFisica = BigDecimal.ZERO;
            categoria = null;
            arancel = null;
        }

        PagoEntity pago = PagoEntity.builder()
                .socio(socio)
                .socioDisciplina(socioDisciplina)
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
            socioDisciplina.setInscripcionPagada(true);
            socioDisciplinaRepository.save(socioDisciplina);
        }

        if ("CUOTA_MENSUAL".equals(concepto) && periodo != null) {
            BigDecimal totalPagadoPeriodo = safe(
                    pagoRepository.sumarMontoPagadoPorPeriodo(
                            socioDisciplina.getId(),
                            "CUOTA_MENSUAL",
                            periodo));

            BigDecimal montoEsperadoPeriodo = calcularMontoEsperadoCuota(socio, socioDisciplina, periodo);

            if (totalPagadoPeriodo.compareTo(montoEsperadoPeriodo) >= 0) {
                YearMonth ym = YearMonth.parse(periodo, YYYY_MM);
                LocalDate finMes = ym.atEndOfMonth();

                if (socioDisciplina.getVigenciaHasta() == null || socioDisciplina.getVigenciaHasta().isBefore(finMes)) {
                    socioDisciplina.setVigenciaHasta(finMes);
                    socioDisciplinaRepository.save(socioDisciplina);
                }
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
        if (socio == null) {
            return new BaseResponse<>("Socio no encontrado", 404, null);
        }

        var pagos = buscarPagosPorRango(socioId, desde, hasta);
        var data = pagos.stream().map(this::toPagoDto).toList();
        return new BaseResponse<>("OK", 200, data);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<PagoDto>> misPagos(String email, LocalDate desde, LocalDate hasta) {

        UsuarioEntity user = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return new BaseResponse<>("Usuario no encontrado", 404, null);
        }

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
        if (socio == null) {
            return BaseResponse.bad("Socio no encontrado");
        }

        List<SocioDisciplinaEntity> relaciones = socioDisciplinaRepository.findBySocio_IdAndActivoTrue(socioId);
        SocioDisciplinaEntity principal = relaciones.isEmpty() ? null : relaciones.get(0);

        int lim = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);

        List<PagoEntity> pagos;
        if (desde != null || hasta != null) {
            pagos = buscarPagosPorRango(socioId, desde, hasta);
            if (pagos.size() > lim) {
                pagos = pagos.subList(0, lim);
            }
        } else {
            pagos = pagoRepository.findBySocio_IdOrderByFechaPagoDesc(
                    socioId,
                    PageRequest.of(0, lim));
        }

        var ultimos = pagos.stream().map(this::toPagoDto).toList();

        List<SocioDisciplinaResumenDto> disciplinas = relaciones.stream()
                .map(sd -> SocioDisciplinaResumenDto.builder()
                        .socioDisciplinaId(sd.getId())
                        .disciplinaId(sd.getDisciplina() != null ? sd.getDisciplina().getId() : null)
                        .disciplinaNombre(sd.getDisciplina() != null ? sd.getDisciplina().getNombre() : null)
                        .arancelDisciplinaId(
                                sd.getArancelDisciplina() != null ? sd.getArancelDisciplina().getId() : null)
                        .categoriaArancel(
                                sd.getArancelDisciplina() != null ? sd.getArancelDisciplina().getCategoria() : null)
                        .vigenciaHasta(sd.getVigenciaHasta())
                        .inscripcionPagada(sd.getInscripcionPagada())
                        .deuda(calcularDeudaPorDisciplina(socio, sd))
                        .tieneBeca(socio.getTieneBeca())
                        .porcentajeBecaSocial(socio.getPorcentajeBecaSocial())
                        .porcentajeBecaDeportiva(socio.getPorcentajeBecaDeportiva())
                        .porcentajeBecaPreparacionFisica(socio.getPorcentajeBecaPreparacionFisica())
                        .observacionBeca(socio.getObservacionBeca())
                        .build())
                .toList();

        DeudaResponseDto deudaPrincipal = principal != null
                ? calcularDeudaPorDisciplina(socio, principal)
                : null;

        SocioResumenDto resumen = SocioResumenDto.builder()
                .socioId(socio.getId())
                .dni(socio.getDni())
                .nombre(socio.getNombre())
                .apellido(socio.getApellido())
                .celular(socio.getCelular())
                .activo(socio.getActivo())
                .vigenciaHasta(principal != null ? principal.getVigenciaHasta() : null)
                .disciplinaId(
                        principal != null && principal.getDisciplina() != null
                                ? principal.getDisciplina().getId()
                                : null)
                .disciplinaNombre(
                        principal != null && principal.getDisciplina() != null
                                ? principal.getDisciplina().getNombre()
                                : null)
                .arancelDisciplinaId(
                        principal != null && principal.getArancelDisciplina() != null
                                ? principal.getArancelDisciplina().getId()
                                : null)
                .categoriaArancel(
                        principal != null && principal.getArancelDisciplina() != null
                                ? principal.getArancelDisciplina().getCategoria()
                                : null)
                .deuda(deudaPrincipal)
                .ultimosPagos(ultimos)
                .disciplinas(disciplinas)
                .tieneBeca(socio.getTieneBeca())
                .porcentajeBecaSocial(socio.getPorcentajeBecaSocial())
                .porcentajeBecaDeportiva(socio.getPorcentajeBecaDeportiva())
                .porcentajeBecaPreparacionFisica(socio.getPorcentajeBecaPreparacionFisica())
                .observacionBeca(socio.getObservacionBeca())
                .build();

        return BaseResponse.ok("Resumen OK", resumen);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<ArancelDisciplinaDto>> listarArancelesActivos() {
        List<ArancelDisciplinaDto> data = arancelDisciplinaRepository
                .findByActivaTrueOrderByDisciplina_IdAscCategoriaAsc()
                .stream()
                .map(this::toArancelDto)
                .toList();

        return BaseResponse.ok("Aranceles OK", data);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<ArancelDisciplinaDto>> listarArancelesPorDisciplina(Long disciplinaId) {
        List<ArancelDisciplinaDto> data = arancelDisciplinaRepository
                .findByDisciplina_IdAndActivaTrueOrderByCategoriaAsc(disciplinaId)
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
    @Transactional(readOnly = true)
    public byte[] generarComprobantePdf(Long pagoId) {
        PagoEntity pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        SocioEntity socio = pago.getSocio();
        if (socio == null) {
            throw new IllegalArgumentException("El pago no tiene socio asociado");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A6, 24, 24, 24, 24);
            PdfWriter.getInstance(document, out);
            document.open();

            try {
                ClassPathResource resource = new ClassPathResource("static/img/logo-club-banco.png");
                InputStream is = resource.getInputStream();
                byte[] bytes = is.readAllBytes();
                Image logo = Image.getInstance(bytes);
                logo.scaleToFit(60, 60);
                logo.setAlignment(Image.ALIGN_CENTER);
                document.add(logo);
            } catch (Exception e) {
                // sigue sin romper si no encuentra logo
            }

            Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            Paragraph titulo = new Paragraph("CLUB BANCO", titleFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(8f);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("COMPROBANTE DE PAGO", boldFont);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(12f);
            document.add(subtitulo);

            DateTimeFormatter fechaHoraFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String fechaHora = pago.getFechaPago() != null ? pago.getFechaPago().format(fechaHoraFmt) : "-";

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5f);
            table.setSpacingAfter(10f);
            table.setWidths(new float[] { 40f, 60f });

            addRow(table, "N° comprobante", String.format("%06d", pago.getId()), boldFont, normalFont);
            addRow(table, "Fecha", fechaHora, boldFont, normalFont);
            addRow(table, "Socio", socio.getApellido() + ", " + socio.getNombre(), boldFont, normalFont);
            addRow(table, "DNI", socio.getDni(), boldFont, normalFont);
            addRow(table, "Disciplina", pago.getDisciplina() != null ? pago.getDisciplina().getNombre() : "-", boldFont,
                    normalFont);
            addRow(table, "Categoría", pago.getCategoria() != null ? pago.getCategoria() : "-", boldFont, normalFont);
            addRow(table, "Concepto", pago.getConcepto() != null ? pago.getConcepto() : "-", boldFont, normalFont);
            addRow(table, "Período", pago.getPeriodo() != null ? pago.getPeriodo() : "-", boldFont, normalFont);
            addRow(table, "Medio", pago.getMedio() != null ? pago.getMedio().name() : "-", boldFont, normalFont);

            document.add(table);

            Paragraph detalleTitulo = new Paragraph("Detalle", boldFont);
            detalleTitulo.setSpacingAfter(6f);
            document.add(detalleTitulo);

            PdfPTable detalle = new PdfPTable(2);
            detalle.setWidthPercentage(100);
            detalle.setWidths(new float[] { 65f, 35f });
            detalle.setSpacingAfter(10f);

            addRow(detalle, "Cuota social", "$ " + safe(pago.getMontoSocial()), normalFont, normalFont);
            addRow(detalle, "Deportivo", "$ " + safe(pago.getMontoDisciplina()), normalFont, normalFont);
            addRow(detalle, "Prep. física", "$ " + safe(pago.getMontoPreparacionFisica()), normalFont, normalFont);

            document.add(detalle);

            Paragraph total = new Paragraph(
                    "TOTAL: $ " + safe(pago.getMontoTotal()),
                    new Font(Font.HELVETICA, 12, Font.BOLD));
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingAfter(10f);
            document.add(total);

            if (pago.getObservacion() != null && !pago.getObservacion().isBlank()) {
                Paragraph obs = new Paragraph("Observación: " + pago.getObservacion(), normalFont);
                obs.setSpacingBefore(6f);
                document.add(obs);
            }

            Paragraph pie = new Paragraph("Comprobante válido como constancia de pago.", normalFont);
            pie.setAlignment(Element.ALIGN_CENTER);
            pie.setSpacingBefore(16f);
            document.add(pie);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el comprobante PDF", e);
        }
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
                toArancelDto(saved));
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
                .anulado(p.getAnulado())
                .fechaAnulacion(p.getFechaAnulacion())
                .motivoAnulacion(p.getMotivoAnulacion())
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

    @Override
    @Transactional
    public BaseResponse<Void> anularPago(Long pagoId, String motivo) {
        PagoEntity pago = pagoRepository.findById(pagoId).orElse(null);
        if (pago == null) {
            return new BaseResponse<>("Pago no encontrado", 404, null);
        }

        if (Boolean.TRUE.equals(pago.getAnulado())) {
            return new BaseResponse<>("El pago ya está anulado", 400, null);
        }

        pago.setAnulado(true);
        pago.setFechaAnulacion(LocalDateTime.now());
        pago.setMotivoAnulacion(
                motivo != null && !motivo.trim().isBlank()
                        ? motivo.trim()
                        : "Anulado manualmente");

        pagoRepository.save(pago);

        SocioDisciplinaEntity sd = pago.getSocioDisciplina();
        if (sd != null) {
            if ("CUOTA_MENSUAL".equalsIgnoreCase(pago.getConcepto())) {
                recalcularVigenciaSocioDisciplina(sd);
            } else if ("INSCRIPCION".equalsIgnoreCase(pago.getConcepto())) {
                recalcularInscripcionSocioDisciplina(sd);
            }
        }

        return new BaseResponse<>("Pago anulado correctamente", 200, null);
    }

    private void recalcularVigenciaSocioDisciplina(SocioDisciplinaEntity sd) {
        if (sd == null || sd.getId() == null || sd.getSocio() == null) {
            return;
        }

        List<String> periodos = pagoRepository.findPeriodosCuotasPagadas(sd.getId());

        LocalDate nuevaVigenciaHasta = null;

        for (String periodo : periodos) {
            if (periodo == null || !periodo.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
                continue;
            }

            BigDecimal totalPagado = safe(
                    pagoRepository.sumarMontoPagadoPorPeriodo(sd.getId(), "CUOTA_MENSUAL", periodo));

            BigDecimal montoEsperado = calcularMontoEsperadoCuota(sd.getSocio(), sd, periodo);

            if (totalPagado.compareTo(montoEsperado) >= 0) {
                LocalDate finMes = YearMonth.parse(periodo, YYYY_MM).atEndOfMonth();

                if (nuevaVigenciaHasta == null || finMes.isAfter(nuevaVigenciaHasta)) {
                    nuevaVigenciaHasta = finMes;
                }
            }
        }

        sd.setVigenciaHasta(nuevaVigenciaHasta);
        socioDisciplinaRepository.save(sd);
    }

    private void recalcularInscripcionSocioDisciplina(SocioDisciplinaEntity sd) {
        if (sd == null || sd.getId() == null) {
            return;
        }

        boolean tieneInscripcionActiva = pagoRepository
                .existsBySocioDisciplina_IdAndConceptoAndAnuladoFalse(sd.getId(), "INSCRIPCION");

        sd.setInscripcionPagada(tieneInscripcionActiva);
        socioDisciplinaRepository.save(sd);
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(4f);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(4f);

        table.addCell(c1);
        table.addCell(c2);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal aplicarBeca(BigDecimal montoBase, BigDecimal porcentajeBeca) {
        BigDecimal base = safe(montoBase);
        BigDecimal beca = safe(porcentajeBeca);

        if (beca.compareTo(BigDecimal.ZERO) <= 0) {
            return base;
        }

        if (beca.compareTo(BigDecimal.valueOf(100)) > 0) {
            beca = BigDecimal.valueOf(100);
        }

        BigDecimal descuento = base
                .multiply(beca)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        return base.subtract(descuento);
    }

    private BigDecimal calcularMontoArancelConBeca(
            ArancelDisciplinaEntity a,
            SocioEntity socio,
            boolean esPrimeraDisciplina) {
        if (a == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal social = esPrimeraDisciplina
                ? aplicarBeca(a.getMontoSocial(), socio.getPorcentajeBecaSocial())
                : BigDecimal.ZERO;

        BigDecimal deportivo = aplicarBeca(a.getMontoDeportivo(), socio.getPorcentajeBecaDeportiva());
        BigDecimal preparacion = aplicarBeca(
                a.getMontoPreparacionFisica(),
                socio.getPorcentajeBecaPreparacionFisica());

        return safe(social).add(safe(deportivo)).add(safe(preparacion));
    }

    private BigDecimal calcularMontoArancel(ArancelDisciplinaEntity a) {
        if (a == null) {
            return BigDecimal.ZERO;
        }

        return safe(a.getMontoSocial())
                .add(safe(a.getMontoDeportivo()))
                .add(safe(a.getMontoPreparacionFisica()));
    }

    private BigDecimal calcularMontoArancelSegunOrden(ArancelDisciplinaEntity a, boolean esPrimeraDisciplina) {
        if (a == null) {
            return BigDecimal.ZERO;
        }

        if (esPrimeraDisciplina) {
            return safe(a.getMontoSocial())
                    .add(safe(a.getMontoDeportivo()))
                    .add(safe(a.getMontoPreparacionFisica()));
        }

        return safe(a.getMontoDeportivo())
                .add(safe(a.getMontoPreparacionFisica()));
    }

    private BigDecimal calcularMontoEsperadoCuota(SocioEntity socio, SocioDisciplinaEntity socioDisciplina,
            String periodo) {
        if (socioDisciplina == null || socioDisciplina.getDisciplina() == null
                || socioDisciplina.getArancelDisciplina() == null) {
            return BigDecimal.ZERO;
        }

        YearMonth ym = YearMonth.parse(periodo, YYYY_MM);
        LocalDate fechaPeriodo = ym.atEndOfMonth();

        String categoria = socioDisciplina.getArancelDisciplina().getCategoria();
        Long disciplinaId = socioDisciplina.getDisciplina().getId();

        ArancelDisciplinaEntity arancelPeriodo = arancelDisciplinaRepository
                .findTopByDisciplina_IdAndCategoriaIgnoreCaseAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                        disciplinaId,
                        categoria,
                        fechaPeriodo)
                .orElse(socioDisciplina.getArancelDisciplina());

        List<SocioDisciplinaEntity> relacionesActivas = socioDisciplinaRepository
                .findBySocio_IdAndActivoTrue(socio.getId());

        Long principalId = relacionesActivas.isEmpty() ? null : relacionesActivas.get(0).getId();
        boolean esPrimeraDisciplina = Objects.equals(socioDisciplina.getId(), principalId);

        return calcularMontoArancelConBeca(arancelPeriodo, socio, esPrimeraDisciplina);
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

    private static String normalize(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }
}