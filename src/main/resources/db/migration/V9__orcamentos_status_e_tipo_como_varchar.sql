-- ============================================================
-- V9__orcamentos_status_e_tipo_como_varchar.sql
--
-- Converte os campos de status/tipo de orçamento de enum nativo
-- para varchar, evitando incompatibilidade de rótulos entre ambientes.
--
-- Princípios:
--   * idempotente;
--   * aditiva: nenhum dado existente é apagado;
--   * não altera V1 a V8.
-- ============================================================

drop view if exists public.vw_orcamento_resumo cascade;

alter table public.orcamentos
  alter column status type varchar(20) using status::text;

alter table public.orcamento_itens
  alter column tipo type varchar(10) using tipo::text;
