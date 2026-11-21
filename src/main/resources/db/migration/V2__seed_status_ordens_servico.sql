-- ============================================================
-- V2__seed_status_ordens_servico.sql
--
-- Dados de referência da OS. A baseline (V1) consolidou o schema, mas não
-- incluiu os inserts das migrações antigas. Sem estes status, a criação de
-- OS falha com "Status 'ABERTA' não encontrado".
--
-- Idempotente: pode rodar em banco novo ou já populado.
-- Estado final igual ao das migrações V1 a V9 anteriores.
-- ============================================================

insert into public.status_ordens_servico (nome, ordem, ativo) values
  ('ABERTA',               1,  true),
  ('EM_ORCAMENTO',         2,  true),
  ('EM_DIAGNOSTICO',       3,  true),
  ('AGUARDANDO_APROVACAO', 4,  true),
  ('AGUARDANDO_PECA',      5,  true),
  ('EM_EXECUCAO',          6,  true),
  ('EM_TESTE',             7,  true),
  ('PRONTA',               8,  true),
  ('CONCLUIDA',            9,  false),
  ('ENTREGUE',             10, true),
  ('CANCELADA',            11, true)
on conflict (nome) do update
  set ordem = excluded.ordem,
      ativo = excluded.ativo;
