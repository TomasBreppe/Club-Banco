package com.example.demo.repository;

import com.example.demo.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    @Query("""
   SELECT u FROM UsuarioEntity u
   LEFT JOIN FETCH u.socios s
   LEFT JOIN FETCH s.disciplina
   WHERE UPPER(u.email) = UPPER(:email)
""")
    Optional<UsuarioEntity> findByEmailWithSocios(@Param("email") String email);
}
