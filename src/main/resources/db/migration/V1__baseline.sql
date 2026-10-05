-- ============================================================
-- V1__baseline.sql
-- Schema da aplicação de gestão de oficinas.
-- Idempotente: roda tanto em banco vazio quanto no schema já existente.
-- ============================================================

-- ---------- Tipos enumerados ----------
do $$ begin
    create type public.prioridade_os as enum ('BAIXA','NORMAL','ALTA','URGENTE');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.status_checklist as enum ('NAO_VERIFICADO','CONFORME','NAO_CONFORME','NAO_APLICAVEL');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.status_orcamento as enum ('RASCUNHO','ENVIADO','APROVADO','REJEITADO','CONVERTIDO','CANCELADO');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.status_aprovacao as enum ('PENDENTE','APROVADO','REJEITADO');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.tipo_orcamento_item as enum ('PECA','SERVICO');
exception when duplicate_object then null; end $$;

-- ---------- Núcleo ----------
create table if not exists public.oficinas (
  id bigint generated always as identity primary key,
  nome varchar not null,
  cnpj varchar unique,
  telefone varchar,
  ativa boolean not null default true,
  criada_em timestamptz not null default now()
);

create table if not exists public.usuarios (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  auth_user_id uuid unique,
  nome varchar not null,
  email varchar not null,
  cargo varchar not null,
  ativo boolean not null default true,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists ix_usuarios_oficina on public.usuarios(oficina_id);

create table if not exists public.clientes (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  nome varchar not null,
  cpf_cnpj varchar not null,
  email varchar,
  telefone varchar not null,
  telefone_secundario varchar,
  classificacao varchar,
  ativo boolean not null default true,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists ix_clientes_oficina on public.clientes(oficina_id);
create index if not exists ix_clientes_oficina_nome on public.clientes(oficina_id, nome);
-- um CPF/CNPJ por oficina (o schema original permitia duplicar)
create unique index if not exists ux_clientes_oficina_cpf_cnpj on public.clientes(oficina_id, cpf_cnpj);

create table if not exists public.enderecos_clientes (
  id bigint generated always as identity primary key,
  cliente_id bigint not null references public.clientes(id) on delete cascade,
  cep varchar not null,
  logradouro varchar not null,
  numero varchar not null,
  complemento varchar,
  bairro varchar not null,
  cidade varchar not null,
  uf char(2) not null,
  principal boolean not null default true
);
create index if not exists ix_enderecos_clientes_cliente on public.enderecos_clientes(cliente_id);

create table if not exists public.veiculos (
  id bigint generated always as identity primary key,
  cliente_id bigint not null references public.clientes(id),
  placa varchar not null unique,
  chassi varchar unique,
  marca varchar not null,
  modelo varchar not null,
  versao varchar,
  ano_fabricacao smallint,
  ano_modelo smallint,
  cor varchar,
  combustivel varchar,
  motorizacao varchar,
  transmissao varchar,
  km_atual integer not null default 0 check (km_atual >= 0),
  foto_url varchar,
  ativo boolean not null default true,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists ix_veiculos_cliente on public.veiculos(cliente_id);

create table if not exists public.boxes (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  nome varchar not null,
  descricao varchar,
  ativo boolean not null default true
);
create index if not exists ix_boxes_oficina on public.boxes(oficina_id);

create table if not exists public.status_ordens_servico (
  id smallint generated always as identity primary key,
  nome varchar not null unique,
  ordem smallint not null default 0,
  ativo boolean not null default true
);

create table if not exists public.servicos (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  codigo varchar not null,
  nome varchar not null,
  descricao text,
  preco_base numeric not null default 0 check (preco_base >= 0),
  ativo boolean not null default true,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists ix_servicos_oficina on public.servicos(oficina_id);

create table if not exists public.pecas (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  codigo varchar not null,
  nome varchar not null,
  descricao text,
  fabricante varchar,
  preco_custo numeric not null default 0 check (preco_custo >= 0),
  preco_venda numeric not null default 0 check (preco_venda >= 0),
  estoque_atual numeric not null default 0 check (estoque_atual >= 0),
  estoque_minimo numeric not null default 0 check (estoque_minimo >= 0),
  ativo boolean not null default true,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists ix_pecas_oficina on public.pecas(oficina_id);

create table if not exists public.ordens_servico (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  numero integer not null,
  cliente_id bigint not null references public.clientes(id),
  veiculo_id bigint not null references public.veiculos(id),
  box_id bigint references public.boxes(id),
  usuario_abertura_id bigint references public.usuarios(id),
  mecanico_lider_id bigint references public.usuarios(id),
  status_id smallint not null references public.status_ordens_servico(id),
  prioridade public.prioridade_os not null default 'NORMAL',
  km_entrada integer check (km_entrada is null or km_entrada >= 0),
  diagnostico text,
  observacoes text,
  desconto numeric not null default 0 check (desconto >= 0),
  garantia_dias integer check (garantia_dias is null or garantia_dias >= 0),
  garantia_km integer check (garantia_km is null or garantia_km >= 0),
  aberta_em timestamptz not null default now(),
  fechada_em timestamptz,
  criada_em timestamptz not null default now(),
  atualizada_em timestamptz not null default now(),
  constraint ux_ordens_servico_oficina_numero unique (oficina_id, numero)
);
create index if not exists ix_ordens_servico_oficina on public.ordens_servico(oficina_id, numero desc);
create index if not exists ix_ordens_servico_status on public.ordens_servico(status_id);
create index if not exists ix_ordens_servico_cliente on public.ordens_servico(cliente_id);
create index if not exists ix_ordens_servico_veiculo on public.ordens_servico(veiculo_id);

create table if not exists public.os_servicos (
  id bigint generated always as identity primary key,
  ordem_servico_id bigint not null references public.ordens_servico(id) on delete cascade,
  servico_id bigint references public.servicos(id),
  mecanico_id bigint references public.usuarios(id),
  descricao varchar not null,
  quantidade numeric not null default 1 check (quantidade > 0),
  valor_unitario numeric not null check (valor_unitario >= 0),
  tempo_estimado_horas numeric,
  criado_em timestamptz not null default now()
);
create index if not exists ix_os_servicos_os on public.os_servicos(ordem_servico_id);

create table if not exists public.os_pecas (
  id bigint generated always as identity primary key,
  ordem_servico_id bigint not null references public.ordens_servico(id) on delete cascade,
  peca_id bigint references public.pecas(id),
  descricao varchar not null,
  quantidade numeric not null check (quantidade > 0),
  valor_unitario numeric not null check (valor_unitario >= 0),
  criado_em timestamptz not null default now()
);
create index if not exists ix_os_pecas_os on public.os_pecas(ordem_servico_id);

create table if not exists public.os_mecanicos (
  id bigint generated always as identity primary key,
  ordem_servico_id bigint not null references public.ordens_servico(id) on delete cascade,
  usuario_id bigint not null references public.usuarios(id),
  funcao varchar
);
create index if not exists ix_os_mecanicos_os on public.os_mecanicos(ordem_servico_id);

create table if not exists public.os_historico (
  id bigint generated always as identity primary key,
  ordem_servico_id bigint not null references public.ordens_servico(id) on delete cascade,
  usuario_id bigint references public.usuarios(id),
  status_anterior_id smallint references public.status_ordens_servico(id),
  status_novo_id smallint references public.status_ordens_servico(id),
  descricao text not null,
  criado_em timestamptz not null default now()
);
create index if not exists ix_os_historico_os on public.os_historico(ordem_servico_id, criado_em);

create table if not exists public.checklist_itens (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  nome varchar not null,
  descricao varchar,
  ativo boolean not null default true
);
create index if not exists ix_checklist_itens_oficina on public.checklist_itens(oficina_id);

create table if not exists public.os_checklist (
  id bigint generated always as identity primary key,
  ordem_servico_id bigint not null references public.ordens_servico(id) on delete cascade,
  checklist_item_id bigint not null references public.checklist_itens(id),
  status public.status_checklist not null default 'NAO_VERIFICADO',
  observacao text
);
create index if not exists ix_os_checklist_os on public.os_checklist(ordem_servico_id);

create table if not exists public.orcamentos (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  numero integer not null,
  cliente_id bigint not null references public.clientes(id),
  veiculo_id bigint not null references public.veiculos(id),
  criado_por bigint references public.usuarios(id),
  ordem_servico_id bigint references public.ordens_servico(id),
  status public.status_orcamento not null default 'RASCUNHO',
  validade date,
  observacoes text,
  desconto numeric not null default 0 check (desconto >= 0),
  emitido_em timestamptz,
  aprovado_em timestamptz,
  rejeitado_em timestamptz,
  motivo_rejeicao text,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  constraint ux_orcamentos_oficina_numero unique (oficina_id, numero)
);
create index if not exists ix_orcamentos_oficina on public.orcamentos(oficina_id, numero desc);

create table if not exists public.orcamento_itens (
  id bigint generated always as identity primary key,
  orcamento_id bigint not null references public.orcamentos(id) on delete cascade,
  tipo public.tipo_orcamento_item not null,
  peca_id bigint references public.pecas(id),
  servico_id bigint references public.servicos(id),
  codigo varchar,
  descricao varchar not null,
  quantidade numeric not null check (quantidade > 0),
  valor_unitario numeric not null check (valor_unitario >= 0),
  criado_em timestamptz not null default now()
);
create index if not exists ix_orcamento_itens_orcamento on public.orcamento_itens(orcamento_id);

create table if not exists public.orcamento_aprovacoes (
  id bigint generated always as identity primary key,
  orcamento_id bigint not null references public.orcamentos(id) on delete cascade,
  status public.status_aprovacao not null default 'PENDENTE',
  token uuid not null default gen_random_uuid() unique,
  aprovado_em timestamptz,
  motivo text,
  ip inet,
  criado_em timestamptz not null default now()
);
create index if not exists ix_orcamento_aprovacoes_orcamento on public.orcamento_aprovacoes(orcamento_id);

create table if not exists public.estoque_movimentacoes (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id),
  peca_id bigint not null references public.pecas(id),
  tipo varchar not null,
  quantidade numeric not null check (quantidade > 0),
  ordem_servico_id bigint references public.ordens_servico(id),
  usuario_id bigint references public.usuarios(id),
  observacao varchar,
  criado_em timestamptz not null default now()
);
create index if not exists ix_estoque_mov_oficina on public.estoque_movimentacoes(oficina_id, criado_em desc);
create index if not exists ix_estoque_mov_peca on public.estoque_movimentacoes(peca_id);

-- ---------- Trigger: manter atualizada_em sozinha ----------
-- Postgres NÃO atualiza automaticamente uma coluna com default now().
-- Sem isso, "atualizada_em" ficaria congelada no valor do INSERT.
create or replace function public.fn_atualiza_atualizada_em()
returns trigger
language plpgsql
as $$
begin
  new.atualizada_em := now();
  return new;
end;
$$;

do $$
declare
  t text;
begin
  foreach t in array array['clientes', 'veiculos', 'usuarios', 'servicos', 'pecas', 'ordens_servico', 'orcamentos']
  loop
    if not exists (
      select 1 from pg_trigger
      where tgname = 'trg_' || t || '_atualizada_em'
        and tgrelid = t::regclass
    ) then
      execute format(
        'create trigger trg_%I_atualizada_em before update on public.%I
         for each row execute function public.fn_atualiza_atualizada_em()',
        t, t
      );
    end if;
  end loop;
end $$;

-- ---------- Seed: status padrão da OS ----------
insert into public.status_ordens_servico (nome, ordem) values
  ('ABERTA',               1),
  ('EM DIAGNOSTICO',       2),
  ('AGUARDANDO APROVACAO', 3),
  ('AGUARDANDO PECA',      4),
  ('EM EXECUCAO',          5),
  ('EM TESTE',             6),
  ('CONCLUIDA',            7),
  ('CANCELADA',            8)
on conflict (nome) do nothing;
