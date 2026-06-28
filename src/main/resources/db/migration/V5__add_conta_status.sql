-- V5__add_conta_status.sql
-- Adiciona campo status à tabela conta para suporte a bloqueio/desbloqueio (UC-05)
-- NOTA: VARCHAR com CHECK — NÃO usar CREATE TYPE ... AS ENUM (DA-29)

ALTER TABLE conta
    ADD COLUMN status              VARCHAR(10)   NOT NULL DEFAULT 'ATIVA',
    ADD COLUMN status_motivo       VARCHAR(255),
    ADD COLUMN status_atualizado_em TIMESTAMPTZ;

ALTER TABLE conta
    ADD CONSTRAINT ck_conta_status CHECK (status IN ('ATIVA', 'BLOQUEADA'));
