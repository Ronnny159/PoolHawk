package com.poolhawk.api.repository;

import com.poolhawk.api.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, java.util.UUID> {
    Optional<UsuarioEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}