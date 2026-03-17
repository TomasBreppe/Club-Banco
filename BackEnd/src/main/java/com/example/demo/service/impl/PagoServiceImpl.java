package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.CuotaDisciplinaRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.ParametroRepository;
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


    private void validarMontosDisponibles(SocioEntity socio, String concepto) {
        // si querés hacerlo estricto, descomentá:
        // BigDecimal social = parametroRepository.findById("SOCIAL_MONTO")
        //         .map(ParametroEntity::getValorNum).orElseThrow(() -> new IllegalArgumentException("Falta SOCIAL_MONTO"));
        // BigDecimal inscripcion = parametroRepository.findById("INSCRIPCION_MONTO")
        //         .map(ParametroEntity::getValorNum).orElseThrow(() -> new IllegalArgumentException("Falta INSCRIPCION_MONTO"));
        // BigDecimal cuota = cuotaDisciplinaRepository.findByDisciplinaIdAndActivaTrue(socio.getDisciplina().getId())
        //         .map(CuotaDisciplinaEntity::getMontoTotal)
        //         .orElseThrow(() -> new IllegalArgumentException("No hay cuota activa para la disciplina"));
    }

    @Override
    @Transactional
    public void registrarPagoAprobado(Long socioId, String concepto, String periodo, String mpPaymentId, String rawJson) {

        SocioEntity socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

        String c = concepto == null ? "MENSUALIDAD" : concepto.trim().toUpperCase();
        if ("CUOTA_MENSUAL".equals(c)) c = "MENSUALIDAD";

        BigDecimal social = parametroRepository.findById("SOCIAL_MONTO")
                .map(ParametroEntity::getValorNum).orElse(BigDecimal.ZERO);

        BigDecimal inscripcion = parametroRepository.findById("INSCRIPCION_MONTO")
                .map(ParametroEntity::getValorNum).orElse(BigDecimal.ZERO);

        BigDecimal cuota = cuotaDisciplinaRepository.findTopByDisciplina_IdAndActivaTrueOrderByVigenteDesdeDesc(socio.getDisciplina().getId())
                .map(CuotaDisciplinaEntity::getMontoTotal)
                .orElseThrow(() -> new IllegalArgumentException("No hay cuota activa para la disciplina"));

        if ("MENSUALIDAD".equals(c)) {

            if (periodo == null || periodo.isBlank()) periodo = YearMonth.now().toString();

            // evitar doble impacto del mismo período
            if (pagoRepository.existsBySocio_IdAndConceptoAndPeriodo(socioId, "MENSUALIDAD", periodo)) return;

            pagoRepository.save(PagoEntity.builder()
                    .socio(socio)
                    .concepto("MENSUALIDAD")
                    .periodo(periodo)
                    .montoTotal(cuota)
                    .montoSocial(social)
                    .montoDisciplina(cuota.subtract(social).max(BigDecimal.ZERO))
                    .medio(MedioPago.EFECTIVO)
                    .mpPaymentId(mpPaymentId)
                    .mpStatus("approved")
                    // .rawPayload(rawJson)  // ✅ dejalo comentado si tu entity no lo tiene
                    .build()
            );

            // vigencia = fin de mes del periodo pagado
            socio.setVigenciaHasta(PeriodUtil.endOfMonth(periodo));
            socioRepository.save(socio);
            return;
        }

        if ("INSCRIPCION".equals(c)) {
            if (pagoRepository.existsBySocio_IdAndConcepto(socioId, "INSCRIPCION")) return;

            pagoRepository.save(PagoEntity.builder()
                    .socio(socio)
                    .concepto("INSCRIPCION")
                    .periodo(null)
                    .montoTotal(inscripcion)
                    .montoSocial(BigDecimal.ZERO)
                    .montoDisciplina(BigDecimal.ZERO)
                    .medio(MedioPago.EFECTIVO)
                    .mpPaymentId(mpPaymentId)
                    .mpStatus("approved")
                    // .rawPayload(rawJson)
                    .build()
            );
        }
    }

    @Override
    public BaseResponse<Void> validarCheckout(String email, Long socioId, String concepto) {
        if (email == null || email.isBlank()) return BaseResponse.bad("Usuario no autenticado");
        if (socioId == null) return BaseResponse.bad("socioId es requerido");
        if (concepto == null || concepto.isBlank()) return BaseResponse.bad("concepto es requerido");

        String c = concepto.trim().toUpperCase();
        if ("CUOTA_MENSUAL".equals(c)) c = "MENSUALIDAD";
        if (!"MENSUALIDAD".equals(c) && !"INSCRIPCION".equals(c)) {
            return BaseResponse.bad("Concepto inválido. Use MENSUALIDAD/INSCRIPCION (o CUOTA_MENSUAL)");
        }

        UsuarioEntity u = usuarioRepository.findByEmailWithSocios(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        boolean pertenece = u.getSocios().stream().anyMatch(s -> s.getId().equals(socioId));
        if (!pertenece) return BaseResponse.bad("Ese socio no pertenece a tu usuario");

        SocioEntity socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

        if ("MENSUALIDAD".equals(c) && PeriodUtil.isAlDia(socio.getVigenciaHasta())) {
            return BaseResponse.bad("El socio ya está al día");
        }

        String periodo = YearMonth.now().toString();

        if ("MENSUALIDAD".equals(c) && pagoRepository.existsBySocio_IdAndConceptoAndPeriodo(socioId, "MENSUALIDAD", periodo)) {
            return BaseResponse.bad("Ya está paga la mensualidad del período " + periodo);
        }

        if ("INSCRIPCION".equals(c) && pagoRepository.existsBySocio_IdAndConcepto(socioId, "INSCRIPCION")) {
            return BaseResponse.bad("Este socio ya pagó la inscripción");
        }

        return BaseResponse.ok("OK", null);
    }

}
