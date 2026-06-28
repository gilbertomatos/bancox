package com.bancox.repository;

import com.bancox.entity.TransferenciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferenciaRepository extends JpaRepository<TransferenciaEntity, UUID> {
}
