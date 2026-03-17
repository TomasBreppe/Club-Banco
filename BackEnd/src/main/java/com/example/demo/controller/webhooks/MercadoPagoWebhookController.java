package com.example.demo.controller.webhooks;

import com.example.demo.service.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/mercadopago")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final MercadoPagoService mercadoPagoService;

    // Mercado Pago suele mandar: ?topic=payment&id=123...
    @PostMapping
    public void webhook(@RequestParam(required = false) String topic,
                        @RequestParam(required = false) String id) {
        mercadoPagoService.procesarNotificacionPago(topic, id);
    }
}
