package com.example.demo.mapper;

import com.example.demo.dto.pagos.PagoDashboardDto;
import com.example.demo.entity.PagoEntity;

public class PagoDashboardMapper {

    public static PagoDashboardDto toDto(PagoEntity p) {
        String socioNombreCompleto = "";
        String disciplinaNombre = null;

        if (p.getSocio() != null) {
            socioNombreCompleto =
                    (p.getSocio().getApellido() != null ? p.getSocio().getApellido() : "")
                            + ", " +
                            (p.getSocio().getNombre() != null ? p.getSocio().getNombre() : "");

            if (p.getSocio().getDisciplina() != null) {
                disciplinaNombre = p.getSocio().getDisciplina().getNombre();
            }
        }

        return PagoDashboardDto.builder()
                .id(p.getId())
                .socioId(p.getSocio() != null ? p.getSocio().getId() : null)
                .socioNombreCompleto(socioNombreCompleto.trim())
                .disciplinaNombre(disciplinaNombre)
                .concepto(p.getConcepto())
                .periodo(p.getPeriodo())
                .montoTotal(p.getMontoTotal())
                .medio(p.getMedio() != null ? p.getMedio().name() : null)
                .fechaPago(p.getFechaPago())
                .mpPaymentId(p.getMpPaymentId())
                .mpStatus(p.getMpStatus())
                .build();
    }
}