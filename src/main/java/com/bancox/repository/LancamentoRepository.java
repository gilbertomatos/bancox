package com.bancox.repository;

import com.bancox.entity.LancamentoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LancamentoRepository extends JpaRepository<LancamentoEntity, UUID> {

    @Query("SELECT l FROM LancamentoEntity l WHERE l.contaId = :contaId AND l.criadoEm >= :inicio AND l.criadoEm <= :fim ORDER BY l.criadoEm DESC")
    List<LancamentoEntity> findByContaIdAndPeriodo(
        @Param("contaId") UUID contaId,
        @Param("inicio") Instant inicio,
        @Param("fim") Instant fim,
        Pageable pageable
    );

    @Query("SELECT l FROM LancamentoEntity l WHERE l.contaId = :contaId ORDER BY l.criadoEm DESC")
    List<LancamentoEntity> findTopByContaId(@Param("contaId") UUID contaId, Pageable pageable);
}
