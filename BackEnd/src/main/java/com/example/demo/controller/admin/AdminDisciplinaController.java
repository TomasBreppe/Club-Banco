package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.disciplina.*;
import com.example.demo.service.DisciplinaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/disciplinas")
@RequiredArgsConstructor
public class AdminDisciplinaController {

    @Autowired
    private DisciplinaService disciplinaService;

    @GetMapping
    public BaseResponse<List<DisciplinaDto>> listar() {
        return disciplinaService.listar();
    }

    @PostMapping
    public BaseResponse<DisciplinaDto> crear(@Valid @RequestBody DisciplinaCreateDto dto) {
        return disciplinaService.crear(dto);
    }

    @PutMapping("/{id}")
    public BaseResponse<DisciplinaDto> actualizar(@PathVariable Long id, @Valid @RequestBody DisciplinaCreateDto dto) {
        return disciplinaService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    public BaseResponse<DisciplinaDto> cambiarEstado(@PathVariable Long id, @RequestParam boolean activa) {
        return disciplinaService.cambiarEstado(id, activa);
    }
}
