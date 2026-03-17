package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.disciplina.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DisciplinaService {
    BaseResponse<List<DisciplinaDto>> listar();
    BaseResponse<DisciplinaDto> crear(DisciplinaCreateDto dto);
    BaseResponse<DisciplinaDto> actualizar(Long id, DisciplinaCreateDto dto);
    BaseResponse<DisciplinaDto> cambiarEstado(Long id, boolean activa);
}
