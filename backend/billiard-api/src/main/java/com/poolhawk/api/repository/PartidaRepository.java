package com.poolhawk.api.repository;

import com.poolhawk.api.entity.PartidaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartidaRepository extends JpaRepository<PartidaEntity, String> {
}