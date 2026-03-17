package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.pagos.CreatePreferenceRequest;
import com.example.demo.dto.pagos.CreatePreferenceResponse;

public interface MercadoPagoService {
    BaseResponse<CreatePreferenceResponse> crearPreferencia(CreatePreferenceRequest req);
    void procesarNotificacionPago(String topic, String id);
}
