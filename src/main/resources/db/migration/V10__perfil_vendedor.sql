-- ============================================================
-- V10__perfil_vendedor.sql
--
-- Adiciona o perfil VENDEDOR ao cadastro de usuários.
--
-- Princípios:
--   * idempotente;
--   * aditiva: nenhum dado existente é apagado;
--   * não altera V1 a V9.
-- ============================================================

alter table public.usuarios
  drop constraint if exists usuarios_perfil_chk;

alter table public.usuarios
  add constraint usuarios_perfil_chk
  check (perfil in ('ADMIN', 'FUNCIONARIO', 'VENDEDOR'));
