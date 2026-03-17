package com.example.demo.controller.me;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.pagos.CreatePreferenceRequest;
import com.example.demo.dto.pagos.CreatePreferenceResponse;
import com.example.demo.service.MercadoPagoService;
import com.example.demo.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/pagos")
@RequiredArgsConstructor
public class MePagosController {

    private final PagoService pagoService;
    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/checkout")
    public BaseResponse<CreatePreferenceResponse> checkout(@Valid @RequestBody CreatePreferenceRequest req,
                                                           Authentication auth) {

        BaseResponse<Void> valid = pagoService.validarCheckout(auth.getName(), req.getSocioId(), req.getConcepto());
        if (valid.getStatus() != 200) {
            return BaseResponse.bad(valid.getMensaje());
        }

        return mercadoPagoService.crearPreferencia(req);
    }
}
