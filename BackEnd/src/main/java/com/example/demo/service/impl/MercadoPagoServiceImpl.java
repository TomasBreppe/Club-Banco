package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.pagos.CreatePreferenceRequest;
import com.example.demo.dto.pagos.CreatePreferenceResponse;
import com.example.demo.service.MercadoPagoService;
import com.example.demo.service.PagoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private final PagoService pagoService;

    @Value("${mp.access-token}")
    private String accessToken;

    @Value("${mp.front.success-url}")
    private String successUrl;

    @Value("${mp.front.pending-url}")
    private String pendingUrl;

    @Value("${mp.front.failure-url}")
    private String failureUrl;

    @Override
    public BaseResponse<CreatePreferenceResponse> crearPreferencia(CreatePreferenceRequest req) {

        MercadoPagoConfig.setAccessToken(accessToken);

        Long socioId = req.getSocioId();
        if (socioId == null) return BaseResponse.bad("socioId es requerido");

        String concepto = req.getConcepto() == null ? "MENSUALIDAD" : req.getConcepto().trim().toUpperCase();
        if ("CUOTA_MENSUAL".equals(concepto)) concepto = "MENSUALIDAD";

        if (!"MENSUALIDAD".equals(concepto) && !"INSCRIPCION".equals(concepto)) {
            return BaseResponse.bad("Concepto inválido. Use MENSUALIDAD/INSCRIPCION (o CUOTA_MENSUAL)");
        }

        // MP exige precio. Para MVP: placeholder (el monto real lo impacta PagoService al aprobarse)
        BigDecimal montoCheckout = "INSCRIPCION".equals(concepto)
                ? BigDecimal.valueOf(35000)
                : BigDecimal.valueOf(10000);

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Club Banco - " + concepto)
                .quantity(1)
                .unitPrice(montoCheckout)
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(successUrl)
                .pending(pendingUrl)
                .failure(failureUrl)
                .build();

        PreferenceRequest prefRequest = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                //.autoReturn("approved")
                .metadata(Map.of(
                        "socioId", socioId,
                        "concepto", concepto,
                        "periodo", YearMonth.now().toString()
                ))
                .build();

        try {
            Preference preference = new PreferenceClient().create(prefRequest);

            return BaseResponse.ok("Preferencia creada", CreatePreferenceResponse.builder()
                    .initPoint(preference.getInitPoint())
                    .preferenceId(preference.getId())
                    .build());

        } catch (Exception e) {
            return BaseResponse.bad("Error creando preferencia Mercado Pago: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void procesarNotificacionPago(String topic, String id) {

        if (topic == null || id == null) return;
        if (!"payment".equalsIgnoreCase(topic)) return;

        MercadoPagoConfig.setAccessToken(accessToken);

        try {
            Payment payment = new PaymentClient().get(Long.parseLong(id));

            String status = payment.getStatus();
            if (status == null || !"approved".equalsIgnoreCase(status)) return;

            Map<String, Object> meta = payment.getMetadata();

            Long socioId = (meta != null && meta.get("socioId") != null)
                    ? Long.valueOf(meta.get("socioId").toString())
                    : null;

            String concepto = (meta != null && meta.get("concepto") != null)
                    ? meta.get("concepto").toString()
                    : "MENSUALIDAD";

            String periodo = (meta != null && meta.get("periodo") != null)
                    ? meta.get("periodo").toString()
                    : YearMonth.now().toString();

            String mpPaymentId = String.valueOf(payment.getId());

            if (socioId != null) {
                pagoService.registrarPagoAprobado(socioId, concepto, periodo, mpPaymentId, null);
            }

        } catch (Exception ignored) {
        }
    }
}
