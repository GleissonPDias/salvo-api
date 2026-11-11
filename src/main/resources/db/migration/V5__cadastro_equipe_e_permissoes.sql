-- ============================================================
-- V5__cadastro_equipe_e_permissoes.sql
--
-- Cadastro de funcionários pelo administrador da oficina e
-- separação entre perfil de acesso e função operacional.
--
-- Princípios:
--   * idempotente;
--   * aditiva: nenhum dado existente é apagado;
--   * defaults preservam o comportamento atual;
--   * não altera V1, V2, V3 nem V4.
-- ============================================================

alter table public.usuarios
  add column if not exists perfil varchar(20) not null default 'FUNCIONARIO',
  add column if not exists executa_os boolean not null default true,
  add column if not exists deve_trocar_senha boolean not null default false;

-- Preserva o comportamento atual: quem já era ADMIN continua ADMIN.
update public.usuarios
   set perfil = 'ADMIN'
 where upper(btrim(cargo)) in ('ADMIN', 'DONO', 'PROPRIETARIO');

do $$
begin
  if not exists (
    select 1
      from pg_constraint
     where conname = 'usuarios_perfil_chk'
       and conrelid = 'public.usuarios'::regclass
  ) then
    alter table public.usuarios
      add constraint usuarios_perfil_chk
      check (perfil in ('ADMIN', 'FUNCIONARIO'));
  end if;
end $$;

create index if not exists ix_usuarios_oficina_perfil_ativo
  on public.usuarios (oficina_id, perfil, ativo);

create index if not exists ix_usuarios_oficina_executa_os
  on public.usuarios (oficina_id, executa_os, ativo);
