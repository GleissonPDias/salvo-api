-- ============================================================
-- V8__orcamento_itens_permite_itens_avulsos.sql
--
-- Remove a restrição que exigia peca_id ou servico_id preenchido em
-- orcamento_itens, permitindo itens avulsos (descrição livre).
--
-- Princípios:
--   * idempotente;
--   * aditiva: nenhum dado existente é apagado;
--   * não altera V1 a V7.
-- ============================================================

alter table public.orcamento_itens
  drop constraint if exists orcamento_itens_check;
