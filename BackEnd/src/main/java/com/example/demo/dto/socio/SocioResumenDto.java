package com.example.demo.dto.socio;

import com.example.demo.dto.deuda.DeudaResponseDto;
import com.example.demo.dto.pagos.PagoDto;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocioResumenDto {
    private Long socioId;
    private String dni;
    private String nombre;
    private String apellido;
    private Boolean activo;
    private LocalDate vigenciaHasta;
    private Long disciplinaId;
    private String disciplinaNombre;

    private Long arancelDisciplinaId;
    private String categoriaArancel;

    private DeudaResponseDto deuda;
    private List<PagoDto> ultimosPagos;
}