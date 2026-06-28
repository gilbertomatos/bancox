-- V6__seed_demo_data.sql
-- Dados fictícios para demonstração do BancoX.
--
-- Credenciais de acesso:
--   Ana Lima      CPF 11122233344  senha Bancox@123  (CORRENTISTA — conta ativa)
--   Bruno Costa   CPF 55566677788  senha Bancox@123  (CORRENTISTA — conta ativa)
--   Carla Souza   CPF 99900011122  senha Bancox@123  (CORRENTISTA — conta BLOQUEADA)
--   Demo Operador CPF 12345678900  senha Operador@1  (OPERADOR)

-- ─── IDs fixos para facilitar depuração ──────────────────────────────────────

-- CLIENTEs
-- cliente_ana:    a1a1a1a1-0001-0001-0001-a1a1a1a1a1a1
-- cliente_bruno:  b2b2b2b2-0002-0002-0002-b2b2b2b2b2b2
-- cliente_carla:  c3c3c3c3-0003-0003-0003-c3c3c3c3c3c3

-- CONTAs
-- conta_ana:      a1a1a1a1-1111-1111-1111-a1a1a1a1a1a1
-- conta_bruno:    b2b2b2b2-2222-2222-2222-b2b2b2b2b2b2
-- conta_carla:    c3c3c3c3-3333-3333-3333-c3c3c3c3c3c3

-- TRANSFERÊNCIAs
-- transfer_1:     d4d4d4d4-0001-0001-0001-d4d4d4d4d4d4

-- ─── CLIENTES ────────────────────────────────────────────────────────────────

INSERT INTO cliente (id, nome, cpf, criado_em) VALUES
    ('a1a1a1a1-0001-0001-0001-a1a1a1a1a1a1', 'Ana Lima',    '11122233344', '2026-01-10 09:00:00+00'),
    ('b2b2b2b2-0002-0002-0002-b2b2b2b2b2b2', 'Bruno Costa', '55566677788', '2026-02-15 14:30:00+00'),
    ('c3c3c3c3-0003-0003-0003-c3c3c3c3c3c3', 'Carla Souza', '99900011122', '2026-03-01 11:00:00+00');

-- ─── CONTAS ──────────────────────────────────────────────────────────────────

INSERT INTO conta (id, cliente_id, saldo, status, status_motivo, status_atualizado_em, criado_em) VALUES
    ('a1a1a1a1-1111-1111-1111-a1a1a1a1a1a1',
     'a1a1a1a1-0001-0001-0001-a1a1a1a1a1a1',
     4550.00, 'ATIVA', NULL, NULL,
     '2026-01-10 09:00:00+00'),

    ('b2b2b2b2-2222-2222-2222-b2b2b2b2b2b2',
     'b2b2b2b2-0002-0002-0002-b2b2b2b2b2b2',
     1700.00, 'ATIVA', NULL, NULL,
     '2026-02-15 14:30:00+00'),

    ('c3c3c3c3-3333-3333-3333-c3c3c3c3c3c3',
     'c3c3c3c3-0003-0003-0003-c3c3c3c3c3c3',
     200.00, 'BLOQUEADA', 'Atividade suspeita detectada.', '2026-06-20 10:00:00+00',
     '2026-03-01 11:00:00+00');

-- ─── USUÁRIOS ────────────────────────────────────────────────────────────────
-- Senha "Bancox@123" → hash BCrypt strength 12
-- Senha "Operador@1" → hash BCrypt strength 12

INSERT INTO usuario (id, cpf, senha_hash, perfil, conta_id, ativo, criado_em) VALUES
    ('a1a1a1a1-aaaa-aaaa-aaaa-a1a1a1a1a1a1',
     '11122233344',
     '$2a$12$bY.NanF4SDrp4.0NXovl0OhHC.o/XQMvOD1m.7YZpyZ1hTDyr0hWC',
     'CORRENTISTA',
     'a1a1a1a1-1111-1111-1111-a1a1a1a1a1a1',
     true, '2026-01-10 09:00:00+00'),

    ('b2b2b2b2-bbbb-bbbb-bbbb-b2b2b2b2b2b2',
     '55566677788',
     '$2a$12$bY.NanF4SDrp4.0NXovl0OhHC.o/XQMvOD1m.7YZpyZ1hTDyr0hWC',
     'CORRENTISTA',
     'b2b2b2b2-2222-2222-2222-b2b2b2b2b2b2',
     true, '2026-02-15 14:30:00+00'),

    ('c3c3c3c3-cccc-cccc-cccc-c3c3c3c3c3c3',
     '99900011122',
     '$2a$12$bY.NanF4SDrp4.0NXovl0OhHC.o/XQMvOD1m.7YZpyZ1hTDyr0hWC',
     'CORRENTISTA',
     'c3c3c3c3-3333-3333-3333-c3c3c3c3c3c3',
     true, '2026-03-01 11:00:00+00'),

    ('d4d4d4d4-dddd-dddd-dddd-d4d4d4d4d4d4',
     '12345678900',
     '$2a$12$.VpQa1H3LKRsinW35cKlMuyN0QORILYINQyBrDXepBi09wed98jxq',
     'OPERADOR',
     NULL,
     true, '2026-01-02 08:00:00+00');

-- ─── TRANSFERÊNCIA (Ana → Bruno, R$ 300,00) ──────────────────────────────────

INSERT INTO transferencia (id, conta_origem, conta_destino, valor, criado_em) VALUES
    ('d4d4d4d4-0001-0001-0001-d4d4d4d4d4d4',
     'a1a1a1a1-1111-1111-1111-a1a1a1a1a1a1',
     'b2b2b2b2-2222-2222-2222-b2b2b2b2b2b2',
     300.00,
     '2026-06-10 14:00:00+00');

-- ─── LANÇAMENTOS — Ana Lima ───────────────────────────────────────────────────

INSERT INTO lancamento (id, conta_id, tipo, valor, saldo_apos, transfer_id, criado_em) VALUES

    -- 1. Depósito de salário
    (gen_random_uuid(),
     'a1a1a1a1-1111-1111-1111-a1a1a1a1a1a1',
     'credito', 5000.00, 5000.00, NULL,
     '2026-06-01 08:00:00+00'),

    -- 2. Pagamento de conta
    (gen_random_uuid(),
     'a1a1a1a1-1111-1111-1111-a1a1a1a1a1a1',
     'debito', 150.00, 4850.00, NULL,
     '2026-06-05 10:30:00+00'),

    -- 3. Transferência enviada para Bruno
    (gen_random_uuid(),
     'a1a1a1a1-1111-1111-1111-a1a1a1a1a1a1',
     'transferencia', 300.00, 4550.00,
     'd4d4d4d4-0001-0001-0001-d4d4d4d4d4d4',
     '2026-06-10 14:00:00+00');

-- ─── LANÇAMENTOS — Bruno Costa ───────────────────────────────────────────────

INSERT INTO lancamento (id, conta_id, tipo, valor, saldo_apos, transfer_id, criado_em) VALUES

    -- 1. Depósito de salário
    (gen_random_uuid(),
     'b2b2b2b2-2222-2222-2222-b2b2b2b2b2b2',
     'credito', 2000.00, 2000.00, NULL,
     '2026-06-01 08:00:00+00'),

    -- 2. Pagamento de internet
    (gen_random_uuid(),
     'b2b2b2b2-2222-2222-2222-b2b2b2b2b2b2',
     'debito', 150.00, 1850.00, NULL,
     '2026-06-03 19:00:00+00'),

    -- 3. Transferência recebida de Ana
    (gen_random_uuid(),
     'b2b2b2b2-2222-2222-2222-b2b2b2b2b2b2',
     'transferencia', 300.00, 2150.00,
     'd4d4d4d4-0001-0001-0001-d4d4d4d4d4d4',
     '2026-06-10 14:00:00+00'),

    -- 4. Pagamento de energia
    (gen_random_uuid(),
     'b2b2b2b2-2222-2222-2222-b2b2b2b2b2b2',
     'debito', 450.00, 1700.00, NULL,
     '2026-06-15 11:00:00+00');

-- ─── LANÇAMENTOS — Carla Souza (conta bloqueada) ─────────────────────────────

INSERT INTO lancamento (id, conta_id, tipo, valor, saldo_apos, transfer_id, criado_em) VALUES

    -- 1. Depósito inicial (antes do bloqueio)
    (gen_random_uuid(),
     'c3c3c3c3-3333-3333-3333-c3c3c3c3c3c3',
     'credito', 200.00, 200.00, NULL,
     '2026-06-01 08:00:00+00');
