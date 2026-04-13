package com.example.demo.mapper;

import com.example.demo.dto.socio.SocioDto;
import com.example.demo.entity.SocioDisciplinaEntity;
import com.example.demo.entity.SocioEntity;

public class SocioMapper {

        private SocioMapper() {
        }

        public static SocioDto toDto(SocioEntity socio, SocioDisciplinaEntity sd) {
                return SocioDto.builder()
                                .id(socio.getId())
                                .dni(socio.getDni())
                                .nombre(socio.getNombre())
                                .apellido(socio.getApellido())
                                .genero(socio.getGenero() != null ? socio.getGenero().name() : null)
                                .telefono(socio.getTelefono())
                                .celular(socio.getCelular())
                                .disciplinaId(sd != null && sd.getDisciplina() != null ? sd.getDisciplina().getId()
                                                : null)
                                .disciplinaNombre(sd != null && sd.getDisciplina() != null
                                                ? sd.getDisciplina().getNombre()
                                                : null)
                                .arancelDisciplinaId(sd != null && sd.getArancelDisciplina() != null
                                                ? sd.getArancelDisciplina().getId()
                                                : null)
                                .categoriaArancel(sd != null && sd.getArancelDisciplina() != null
                                                ? sd.getArancelDisciplina().getCategoria()
                                                : null)
                                .vigenciaHasta(sd != null ? sd.getVigenciaHasta() : null)
                                .activo(socio.getActivo())
                                .build();
        }
}