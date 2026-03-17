package com.example.demo.mapper;

import com.example.demo.dto.gasto.GastoDto;
import com.example.demo.entity.GastoEntity;

public class GastoMapper {

    public static GastoDto toDto(GastoEntity g) {
        return GastoDto.builder()
                .id(g.getId())
                .fecha(g.getFecha())
                .categoria(g.getCategoria())
                .concepto(g.getConcepto())
                .descripcion(g.getDescripcion())
                .monto(g.getMonto())
                .medioPago(g.getMedioPago())
                .activo(g.getActivo())
                .createdAt(g.getCreatedAt())
                .build();
    }
}