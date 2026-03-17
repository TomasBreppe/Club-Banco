package com.example.demo.repository;

import com.example.demo.entity.DisciplinaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface DisciplinaRepository extends JpaRepository<DisciplinaEntity, Long> {
    Optional<DisciplinaEntity> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
}