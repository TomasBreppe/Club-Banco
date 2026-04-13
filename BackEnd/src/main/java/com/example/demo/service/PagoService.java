package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.pagos.CreatePreferenceResponse;

public interface PagoService {

    void registrarPagoAprobado(Long socioId, String concepto, String periodo, String mpPaymentId, String rawJson);

    BaseResponse<Void> validarCheckout(String email, Long socioId, String concepto);

    BaseResponse<Void> anularPago(Long pagoId, String motivo);

}
