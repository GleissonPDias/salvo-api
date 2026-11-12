-- ============================================================
-- V7__orcamentos_campos_comerciais.sql
--
-- Campos comerciais do orçamento usados pela tela de Orçamentos.
--
-- Princípios:
--   * idempotente;
--   * aditiva: nenhum dado existente é apagado;
--   * não altera V1 a V6.
-- ============================================================

alter table public.orcamentos
  add column if not exists titulo varchar(120),
  add column if not exists tipo varchar(40),
  add column if not exists validade_dias smallint not null default 7,
  add column if not exists desconto_tipo varchar(20) not null default 'valor',
  add column if not exists canal_envio varchar(20);

update public.orcamentos
   set validade_dias = greatest(1, (validade - (emitido_em::date)))
 where validade is not null and emitido_em is not null and validade_dias = 7;
