package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.ingresos.IngresoManualCreateDto;
import com.example.demo.dto.ingresos.IngresoManualDto;
import com.example.demo.entity.CategoriaIngresoManual;
import com.example.demo.entity.IngresoManualEntity;
import com.example.demo.entity.MedioIngresoManual;
import com.example.demo.repository.IngresoManualRepository;
import com.example.demo.service.IngresoManualService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IngresoManualServiceImpl implements IngresoManualService {

    private final IngresoManualRepository ingresoManualRepository;

    @Override
    public BaseResponse<IngresoManualDto> crear(IngresoManualCreateDto dto) {
        if (dto.getFecha() == null) {
            return new BaseResponse<>("La fecha es obligatoria", 400, null);
        }

        if (dto.getCategoria() == null || dto.getCategoria().trim().isBlank()) {
            return new BaseResponse<>("La categoría es obligatoria", 400, null);
        }

        if (dto.getMonto() == null || dto.getMonto().doubleValue() <= 0) {
            return new BaseResponse<>("El monto debe ser mayor a 0", 400, null);
        }

        if (dto.getMedioPago() == null || dto.getMedioPago().trim().isBlank()) {
            return new BaseResponse<>("El medio de pago es obligatorio", 400, null);
        }

        CategoriaIngresoManual categoria;
        MedioIngresoManual medioPago;

        try {
            categoria = CategoriaIngresoManual.valueOf(dto.getCategoria().trim().toUpperCase().replace(" ", "_"));
        } catch (Exception e) {
            return new BaseResponse<>("La categoría ingresada no es válida", 400, null);
        }

        if (categoria == CategoriaIngresoManual.CUOTAS_ATRASADAS &&
                (dto.getDescripcion() == null || dto.getDescripcion().trim().isBlank())) {
            return new BaseResponse<>("La descripción es obligatoria cuando la categoría es CUOTAS_ATRASADAS", 400, null);
        }

        try {
            medioPago = MedioIngresoManual.valueOf(dto.getMedioPago().trim().toUpperCase());
        } catch (Exception e) {
            return new BaseResponse<>("El medio de pago ingresado no es válido", 400, null);
        }

        IngresoManualEntity entity = IngresoManualEntity.builder()
                .fecha(dto.getFecha())
                .categoria(categoria)
                .medioPago(medioPago)
                .monto(dto.getMonto())
                .descripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null)
                .fechaCreacion(LocalDateTime.now())
                .build();

        entity = ingresoManualRepository.save(entity);

        IngresoManualDto response = IngresoManualDto.builder()
                .id(entity.getId())
                .fecha(entity.getFecha())
                .categoria(entity.getCategoria().name())
                .medioPago(entity.getMedioPago().name())
                .monto(entity.getMonto())
                .descripcion(entity.getDescripcion())
                .build();

        return new BaseResponse<>("Ingreso manual registrado correctamente", 201, response);
    }
}