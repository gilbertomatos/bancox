-- V3__fix_cpf_char_to_varchar.sql
-- Correção: CHAR(11) → VARCHAR(11) nos campos CPF
--
-- Motivo: Hibernate mapeia String Java para VARCHAR por padrão.
-- CHAR(11) no PostgreSQL (bpchar) causa erro de schema-validation no startup:
-- "wrong column type encountered in column [cpf]; found [bpchar], but expecting [varchar(11)]"
--
-- Opção escolhida: VARCHAR(11) — tamanho fixo garantido por validação de aplicação,
-- não pelo tipo de coluna. Mais simples e sem @Column(columnDefinition) nas entidades.
--
-- Referência: DA-02 (nunca editar migration já aplicada — criar nova migration de correção)

ALTER TABLE cliente ALTER COLUMN cpf TYPE VARCHAR(11);
ALTER TABLE usuario ALTER COLUMN cpf TYPE VARCHAR(11);
