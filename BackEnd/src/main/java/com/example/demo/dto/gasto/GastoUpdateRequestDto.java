package com.example.demo.dto.gasto;

import com.example.demo.entity.GastoCategoria;
import com.example.demo.entity.MedioPago;
import com.example.demo.entity.MedioPagoGasto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GastoUpdateRequestDto {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La categoría es obligatoria")
    private GastoCategoria categoria;

    @NotNull(message = "El concepto es obligatorio")
    @Size(max = 120, message = "El concepto no puede superar los 120 caracteres")
    private String concepto;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    @NotNull(message = "El medio de pago es obligatorio")
    private MedioPagoGasto medioPago;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
}