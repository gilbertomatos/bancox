-- V1__init_schema.sql
-- Schema inicial do BancoX
-- Entidades: CLIENTE, CONTA, LANCAMENTO, TRANSFERENCIA
-- Gerado conforme modelo-er/ do template

-- ─── CLIENTE ─────────────────────────────────────────────────────────────────

CREATE TABLE cliente (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    nome       VARCHAR(100) NOT NULL,
    cpf        VARCHAR(11) NOT NULL,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_cliente PRIMARY KEY (id),
    CONSTRAINT uq_cliente_cpf UNIQUE (cpf)
);

-- ─── CONTA ───────────────────────────────────────────────────────────────────

CREATE TABLE conta (
    id          UUID           NOT NULL DEFAULT gen_random_uuid(),
    cliente_id  UUID           NOT NULL,
    saldo       NUMERIC(15,2)  NOT NULL DEFAULT 0 CHECK (saldo >= 0),
    criado_em   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_conta       PRIMARY KEY (id),
    CONSTRAINT fk_conta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

-- ─── TRANSFERENCIA ───────────────────────────────────────────────────────────

CREATE TABLE transferencia (
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    conta_origem   UUID           NOT NULL,
    conta_destino  UUID           NOT NULL,
    valor          NUMERIC(15,2)  NOT NULL CHECK (valor > 0),
    criado_em      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_transferencia          PRIMARY KEY (id),
    CONSTRAINT fk_transferencia_origem   FOREIGN KEY (conta_origem)  REFERENCES conta(id),
    CONSTRAINT fk_transferencia_destino  FOREIGN KEY (conta_destino) REFERENCES conta(id),
    CONSTRAINT ck_contas_distintas       CHECK (conta_origem != conta_destino)
);

-- ─── LANCAMENTO ──────────────────────────────────────────────────────────────
-- NOTA: usar VARCHAR com CHECK — NÃO usar CREATE TYPE ... AS ENUM (DA-29)
-- Hibernate @Enumerated(EnumType.STRING) mapeia para VARCHAR, não para tipo customizado

CREATE TABLE lancamento (
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    conta_id       UUID           NOT NULL,
    tipo           VARCHAR(20)    NOT NULL,
    valor          NUMERIC(15,2)  NOT NULL CHECK (valor > 0),
    saldo_apos     NUMERIC(15,2)  NOT NULL CHECK (saldo_apos >= 0),
    transfer_id    UUID,
    criado_em      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_lancamento              PRIMARY KEY (id),
    CONSTRAINT ck_lancamento_tipo         CHECK (tipo IN ('credito', 'debito', 'transferencia')),
    CONSTRAINT fk_lancamento_conta        FOREIGN KEY (conta_id)    REFERENCES conta(id),
    CONSTRAINT fk_lancamento_transferencia FOREIGN KEY (transfer_id) REFERENCES transferencia(id)
);

-- ─── ÍNDICES ─────────────────────────────────────────────────────────────────

-- Extrato: busca por conta + período (UC-04)
CREATE INDEX idx_lancamento_conta_data ON lancamento (conta_id, criado_em DESC);

-- Rastreabilidade de transferência
CREATE INDEX idx_lancamento_transfer_id ON lancamento (transfer_id) WHERE transfer_id IS NOT NULL;
