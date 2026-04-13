package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.CuotaDisciplinaRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.ParametroRepository;
import com.example.demo.repository.SocioDisciplinaRepository;
import com.example.demo.repository.SocioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.PagoService;
import com.example.demo.util.PeriodUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final UsuarioRepository usuarioRepository;
    private final SocioRepository socioRepository;
    private final CuotaDisciplinaRepository cuotaDisciplinaRepository;
    private final ParametroRepository parametroRepository;
    private final PagoRepository pagoRepository;
    private final SocioDisciplinaRepository socioDisciplinaRepository;

    private void validarMontosDisponibles(SocioEntity socio, String concepto) {
        // reservado
    }

    @Override
    @Transactional
    public void registrarPagoAprobado(Long socioId, String concepto, String periodo, String mpPaymentId,
            String rawJson) {

        SocioEntity socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

        SocioDisciplinaEntity principal = socioDisciplinaRepository
                .findFirstBySocio_IdAndActivoTrueOrderByIdAsc(socioId)
                .orElseThrow(() -> new IllegalArgumentException("El socio no tiene disciplina activa"));

        String c = concepto == null ? "MENSUALIDAD" : concepto.trim().toUpperCase();
        if ("CUOTA_MENSUAL".equals(c))
            c = "MENSUALIDAD";

        BigDecimal social = parametroRepository.findById("SOCIAL_MONTO")
                .map(ParametroEntity::getValorNum)
                .orElse(BigDecimal.ZERO);

        BigDecimal inscripcion = parametroRepository.findById("INSCRIPCION_MONTO")
                .map(ParametroEntity::getValorNum)
                .orElse(BigDecimal.ZERO);

        BigDecimal cuota = cuotaDisciplinaRepository
                .findTopByDisciplina_IdAndActivaTrueOrderByVigenteDesdeDesc(principal.getDisciplina().getId())
                .map(CuotaDisciplinaEntity::getMontoTotal)
                .orElseThrow(() -> new IllegalArgumentException("No hay cuota activa para la disciplina"));

        if ("MENSUALIDAD".equals(c)) {

            if (periodo == null || periodo.isBlank()) {
                periodo = YearMonth.now().toString();
            }

            if (pagoRepository.existsBySocioDisciplina_IdAndConceptoAndPeriodo(
                    principal.getId(),
                    "MENSUALIDAD",
                    periodo)) {
                return;
            }

            pagoRepository.save(PagoEntity.builder()
                    .socio(socio)
                    .socioDisciplina(principal)
                    .disciplina(principal.getDisciplina())
                    .arancelDisciplina(principal.getArancelDisciplina())
                    .concepto("MENSUALIDAD")
                    .periodo(periodo)
                    .categoria(
                            principal.getArancelDisciplina() != null ? principal.getArancelDisciplina().getCategoria()
                                    : null)
                    .montoTotal(cuota)
                    .montoSocial(social)
                    .montoDisciplina(cuota.subtract(social).max(BigDecimal.ZERO))
                    .montoPreparacionFisica(BigDecimal.ZERO)
                    .medio(MedioPago.EFECTIVO)
                    .mpPaymentId(mpPaymentId)
                    .mpStatus("approved")
                    .build());

            principal.setVigenciaHasta(PeriodUtil.endOfMonth(periodo));
            socioDisciplinaRepository.save(principal);
            return;
        }

        if ("INSCRIPCION".equals(c)) {
            if (pagoRepository.existsBySocioDisciplina_IdAndConcepto(principal.getId(), "INSCRIPCION")) {
                return;
            }

            pagoRepository.save(PagoEntity.builder()
                    .socio(socio)
                    .socioDisciplina(principal)
                    .disciplina(principal.getDisciplina())
                    .arancelDisciplina(principal.getArancelDisciplina())
                    .concepto("INSCRIPCION")
                    .periodo(null)
                    .categoria(
                            principal.getArancelDisciplina() != null ? principal.getArancelDisciplina().getCategoria()
                                    : null)
                    .montoTotal(inscripcion)
                    .montoSocial(BigDecimal.ZERO)
                    .montoDisciplina(BigDecimal.ZERO)
                    .montoPreparacionFisica(BigDecimal.ZERO)
                    .medio(MedioPago.EFECTIVO)
                    .mpPaymentId(mpPaymentId)
                    .mpStatus("approved")
                    .build());

            principal.setInscripcionPagada(true);
            socioDisciplinaRepository.save(principal);
        }
    }

    @Override
    public BaseResponse<Void> validarCheckout(String email, Long socioId, String concepto) {
        if (email == null || email.isBlank())
            return BaseResponse.bad("Usuario no autenticado");
        if (socioId == null)
            return BaseResponse.bad("socioId es requerido");
        if (concepto == null || concepto.isBlank())
            return BaseResponse.bad("concepto es requerido");

        String c = concepto.trim().toUpperCase();
        if ("CUOTA_MENSUAL".equals(c))
            c = "MENSUALIDAD";
        if (!"MENSUALIDAD".equals(c) && !"INSCRIPCION".equals(c)) {
            return BaseResponse.bad("Concepto inválido. Use MENSUALIDAD/INSCRIPCION (o CUOTA_MENSUAL)");
        }

        UsuarioEntity u = usuarioRepository.findByEmailWithSocios(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        boolean pertenece = u.getSocios().stream().anyMatch(s -> s.getId().equals(socioId));
        if (!pertenece)
            return BaseResponse.bad("Ese socio no pertenece a tu usuario");

        SocioEntity socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

        SocioDisciplinaEntity principal = socioDisciplinaRepository
                .findFirstBySocio_IdAndActivoTrueOrderByIdAsc(socioId)
                .orElse(null);

        if (principal == null) {
            return BaseResponse.bad("El socio no tiene disciplina activa");
        }

        if ("MENSUALIDAD".equals(c) && PeriodUtil.isAlDia(principal.getVigenciaHasta())) {
            return BaseResponse.bad("El socio ya está al día");
        }

        String periodo = YearMonth.now().toString();

        if ("MENSUALIDAD".equals(c) &&
                pagoRepository.existsBySocioDisciplina_IdAndConceptoAndPeriodo(principal.getId(), "MENSUALIDAD",
                        periodo)) {
            return BaseResponse.bad("Ya está paga la mensualidad del período " + periodo);
        }

        if ("INSCRIPCION".equals(c) &&
                pagoRepository.existsBySocioDisciplina_IdAndConcepto(principal.getId(), "INSCRIPCION")) {
            return BaseResponse.bad("Este socio ya pagó la inscripción");
        }

        return BaseResponse.ok("OK", null);
    }

    private void recalcularEstadoSocioDisciplina(SocioDisciplinaEntity sd) {
        Long socioDisciplinaId = sd.getId();

        boolean tieneInscripcionVigente = pagoRepository.existsBySocioDisciplina_IdAndConceptoAndAnuladoFalse(
                socioDisciplinaId,
                "INSCRIPCION");

        sd.setInscripcionPagada(tieneInscripcionVigente);

        var cuotas = pagoRepository.findCuotasNoAnuladasBySocioDisciplina(socioDisciplinaId);

        String ultimoPeriodo = cuotas.stream()
                .map(PagoEntity::getPeriodo)
                .filter(p -> p != null && !p.isBlank())
                .max(String::compareTo)
                .orElse(null);

        if (ultimoPeriodo == null) {
            sd.setVigenciaHasta(null);
        } else {
            java.time.YearMonth ym = java.time.YearMonth.parse(ultimoPeriodo);
            sd.setVigenciaHasta(ym.atEndOfMonth());
        }

        socioDisciplinaRepository.save(sd);
    }

    @Override
    @Transactional
    public BaseResponse<Void> anularPago(Long pagoId, String motivo) {
        PagoEntity pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (Boolean.TRUE.equals(pago.getAnulado())) {
            return new BaseResponse<>("El pago ya está anulado", 400, null);
        }

        pago.setAnulado(true);
        pago.setFechaAnulacion(java.time.LocalDateTime.now());
        pago.setMotivoAnulacion(
                motivo != null && !motivo.trim().isBlank()
                        ? motivo.trim()
                        : "Anulado manualmente");

        pagoRepository.save(pago);

        SocioDisciplinaEntity sd = pago.getSocioDisciplina();
        if (sd != null) {
            recalcularEstadoSocioDisciplina(sd);
        }

        return new BaseResponse<>("Pago anulado correctamente", 200, null);
    }
}