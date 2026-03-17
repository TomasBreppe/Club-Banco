package com.example.demo.mapper;

import com.example.demo.dto.socio.SocioDto;
import com.example.demo.entity.SocioEntity;

import java.time.LocalDate;

public class SocioMapper {

    public static SocioDto toDto(SocioEntity s) {
        String estado = "DEBE";
        LocalDate vh = s.getVigenciaHasta();
        if (vh != null && !vh.isBefore(LocalDate.now())) estado = "AL_DIA";

        return SocioDto.builder()
                .id(s.getId())
                .dni(s.getDni())
                .nombre(s.getNombre())
                .apellido(s.getApellido())
                .genero(String.valueOf(s.getGenero()))
                .telefono(s.getTelefono())
                .celular(s.getCelular())
                .disciplinaId(s.getDisciplina() != null ? s.getDisciplina().getId() : null)
                .disciplinaNombre(s.getDisciplina() != null ? s.getDisciplina().getNombre() : null)
                .arancelDisciplinaId(s.getArancelDisciplina() != null ? s.getArancelDisciplina().getId() : null)
                .categoriaArancel(s.getArancelDisciplina() != null ? s.getArancelDisciplina().getCategoria() : null)
                .vigenciaHasta(s.getVigenciaHasta())
                .estadoPago(estado)
                .activo(s.getActivo())
                .build();
    }
}