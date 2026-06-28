-- V2__add_usuario.sql
-- Tabela de autenticação — USUARIO
-- Perfis: CORRENTISTA, OPERADOR, ADMIN
-- NOTA: usar VARCHAR com CHECK — NÃO usar CREATE TYPE ... AS ENUM (DA-29)

CREATE TABLE usuario (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    cpf         VARCHAR(11)   NOT NULL,
    senha_hash  VARCHAR(60)   NOT NULL,
    perfil      VARCHAR(20)   NOT NULL,
    conta_id    UUID,
    ativo       BOOLEAN       NOT NULL DEFAULT true,
    criado_em   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_usuario          PRIMARY KEY (id),
    CONSTRAINT ck_usuario_perfil    CHECK (perfil IN ('CORRENTISTA', 'OPERADOR', 'ADMIN')),
    CONSTRAINT uq_usuario_cpf      UNIQUE (cpf),
    CONSTRAINT fk_usuario_conta    FOREIGN KEY (conta_id) REFERENCES conta(id)
);

-- Index para login por CPF (UC-00 — acesso frequente)
CREATE INDEX idx_usuario_cpf ON usuario (cpf);
