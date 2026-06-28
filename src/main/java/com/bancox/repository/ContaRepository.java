package com.bancox.repository;

import com.bancox.entity.ContaEntity;
import com.bancox.entity.StatusConta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<ContaEntity, UUID> {

    List<ContaEntity> findAllByOrderByCriadoEmDesc();

    List<ContaEntity> findByStatusOrderByCriadoEmDesc(StatusConta status);
}
