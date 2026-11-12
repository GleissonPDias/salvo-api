-- ============================================================
-- V6__permissoes_por_modulo_e_auditoria.sql
--
-- Permissões por módulo, marcação do dono da oficina e trilha de
-- auditoria das ações feitas no app.
--
-- Princípios:
--   * idempotente;
--   * aditiva: nenhum dado existente é apagado;
--   * administradores continuam com acesso total;
--   * não altera V1, V2, V3, V4 nem V5.
-- ============================================================

-- ------------------------------------------------------------
-- Dono da oficina: único usuário que faz login direto no app.
-- ------------------------------------------------------------
alter table public.usuarios
  add column if not exists dono boolean not null default false;

-- Em bancos já existentes, o administrador mais antigo de cada
-- oficina passa a ser o dono. Se a oficina ainda não tem dono,
-- mantém o comportamento atual sem quebrar o acesso.
update public.usuarios u
   set dono = true
  from (
        select distinct on (oficina_id) id
          from public.usuarios
         where perfil = 'ADMIN'
         order by oficina_id, criado_em asc, id asc
       ) escolhido
 where u.id = escolhido.id
   and not exists (
        select 1
          from public.usuarios x
         where x.oficina_id = u.oficina_id
           and x.dono
       );

create unique index if not exists ux_usuarios_oficina_dono
  on public.usuarios (oficina_id)
  where dono;

-- ------------------------------------------------------------
-- Permissões por módulo (somente para funcionários).
-- Administradores têm acesso a todos os módulos por definição.
-- ------------------------------------------------------------
create table if not exists public.usuario_permissoes (
  id bigint generated always as identity primary key,
  usuario_id bigint not null references public.usuarios(id) on delete cascade,
  modulo varchar(30) not null,
  criado_em timestamptz not null default now(),
  constraint ux_usuario_permissoes_usuario_modulo unique (usuario_id, modulo),
  constraint usuario_permissoes_modulo_chk check (
    modulo in ('CLIENTES', 'VEICULOS', 'OS', 'ORCAMENTOS', 'ESTOQUE', 'AGENDA', 'FINANCEIRO')
  )
);

create index if not exists ix_usuario_permissoes_usuario
  on public.usuario_permissoes (usuario_id);

-- ------------------------------------------------------------
-- Auditoria: registro append-only das ações de escrita.
-- O nome é copiado no momento da ação para preservar o histórico
-- mesmo se o usuário for renomeado ou removido.
-- ------------------------------------------------------------
create table if not exists public.auditoria (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  usuario_id bigint references public.usuarios(id) on delete set null,
  usuario_nome varchar not null,
  acao varchar(30) not null,
  modulo varchar(30),
  entidade varchar(60),
  entidade_id varchar(60),
  metodo varchar(10) not null,
  caminho varchar(300) not null,
  status_http integer,
  resumo text,
  criado_em timestamptz not null default now()
);

create index if not exists ix_auditoria_oficina_criado_em
  on public.auditoria (oficina_id, criado_em desc);

create index if not exists ix_auditoria_usuario
  on public.auditoria (usuario_id, criado_em desc);

create index if not exists ix_auditoria_modulo
  on public.auditoria (oficina_id, modulo, criado_em desc);
