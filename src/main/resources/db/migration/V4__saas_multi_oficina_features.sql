-- ============================================================
-- V4__saas_multi_oficina_features.sql
-- Evolução para SaaS multi-oficina + features que o front já usa.
--
-- Princípios:
--   * idempotente (pode ser reaplicada sem quebrar);
--   * aditiva: nenhum dado existente é apagado;
--   * colunas novas em tabelas existentes são nullable ou têm default,
--     para o backend atual continuar inserindo enquanto o código novo
--     não é implantado;
--   * não altera V1, V2 nem V3.
--
-- Mapa das decisões de produto:
--   * multi-oficina: veiculos passa a ter oficina_id + unique por oficina;
--   * status PRONTA e ENTREGUE passam a existir separados;
--   * checklist do front (ok/atencao/avaria) é mapeado para o enum
--     status_checklist (CONFORME/NAO_CONFORME/NAO_APLICAVEL);
--   * estoque: trigger BEFORE INSERT aplica a movimentação e trava a peça
--     com SELECT ... FOR UPDATE (seguro contra concorrência);
--   * RLS ligado em public + grants de anon/authenticated revogados.
-- ============================================================


-- ============================================================
-- 1. VEÍCULOS POR OFICINA
-- ============================================================
alter table public.veiculos
  add column if not exists oficina_id bigint;

-- Backfill: todo veículo herda a oficina do cliente.
update public.veiculos v
   set oficina_id = c.oficina_id
  from public.clientes c
 where c.id = v.cliente_id
   and v.oficina_id is null;

do $$ begin
  if not exists (
    select 1
      from pg_constraint
     where conname = 'fk_veiculos_oficina'
       and conrelid = 'public.veiculos'::regclass
  ) then
    alter table public.veiculos
      add constraint fk_veiculos_oficina
      foreign key (oficina_id) references public.oficinas(id);
  end if;
end $$;

create index if not exists ix_veiculos_oficina
  on public.veiculos(oficina_id);
create index if not exists ix_veiculos_placa
  on public.veiculos(placa);

-- Mantém oficina_id coerente mesmo quando o backend antigo (que ainda não
-- mapeia a coluna) inserir ou trocar o cliente do veículo.
create or replace function public.fn_preenche_veiculo_oficina()
returns trigger
language plpgsql
as $$
begin
  if tg_op = 'INSERT' then
    if new.oficina_id is null then
      select c.oficina_id
        into new.oficina_id
        from public.clientes c
       where c.id = new.cliente_id;
    end if;
  elsif new.cliente_id is distinct from old.cliente_id
     or new.oficina_id is null then
    select c.oficina_id
      into new.oficina_id
      from public.clientes c
     where c.id = new.cliente_id;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_veiculos_oficina on public.veiculos;
create trigger trg_veiculos_oficina
  before insert or update of cliente_id, oficina_id on public.veiculos
  for each row execute function public.fn_preenche_veiculo_oficina();

alter table public.veiculos alter column oficina_id set not null;

-- Remove a unicidade global de placa/chassi (incompatível com SaaS).
do $$
declare
  r record;
begin
  for r in
    select c.conname
      from pg_constraint c
     where c.conrelid = 'public.veiculos'::regclass
       and c.contype = 'u'
       and (
         c.conkey = array[(select attnum from pg_attribute
                            where attrelid = 'public.veiculos'::regclass
                              and attname = 'placa')]
         or c.conkey = array[(select attnum from pg_attribute
                               where attrelid = 'public.veiculos'::regclass
                                 and attname = 'chassi')]
       )
  loop
    execute format('alter table public.veiculos drop constraint %I', r.conname);
  end loop;
end $$;

-- Também remove índices únicos soltos com os nomes padrão do Postgres.
drop index if exists public.veiculos_placa_key;
drop index if exists public.veiculos_chassi_key;

-- Unique por oficina, normalizando placa (ABC-1251 = ABC1251) e chassi.
-- Se o banco legado já tiver duplicidade, o índice não é criado para não
-- derrubar a migração; a pendência fica visível para correção manual.
do $$ begin
  if not exists (
    select 1
      from public.veiculos
     group by oficina_id, upper(regexp_replace(placa, '[^A-Za-z0-9]', '', 'g'))
    having count(*) > 1
  ) then
    create unique index if not exists ux_veiculos_oficina_placa
      on public.veiculos(
        oficina_id,
        upper(regexp_replace(placa, '[^A-Za-z0-9]', '', 'g'))
      );
  else
    raise notice 'ux_veiculos_oficina_placa nao criado: existem placas duplicadas na mesma oficina';
  end if;
end $$;

do $$ begin
  if not exists (
    select 1
      from public.veiculos
     where chassi is not null
     group by oficina_id, upper(btrim(chassi))
    having count(*) > 1
  ) then
    create unique index if not exists ux_veiculos_oficina_chassi
      on public.veiculos(oficina_id, upper(btrim(chassi)))
      where chassi is not null;
  else
    raise notice 'ux_veiculos_oficina_chassi nao criado: existem chassis duplicados na mesma oficina';
  end if;
end $$;

alter table public.veiculos
  add column if not exists categoria varchar,
  add column if not exists rascunho boolean not null default false;


-- ============================================================
-- 2. CLIENTES E ENDEREÇOS
-- ============================================================
alter table public.clientes
  add column if not exists whatsapp boolean not null default false,
  add column if not exists aceita_comunicacoes boolean not null default false,
  add column if not exists observacoes text;

alter table public.enderecos_clientes
  add column if not exists criado_em timestamptz not null default now(),
  add column if not exists atualizado_em timestamptz not null default now();

-- Só um endereço principal por cliente. Antes de criar o índice, mantém como
-- principal apenas o endereço mais recente (maior id) de cada cliente, para
-- que dados legados com duplicidade não derrubem a migração.
update public.enderecos_clientes e
   set principal = false
 where e.principal
   and exists (
     select 1
       from public.enderecos_clientes x
      where x.cliente_id = e.cliente_id
        and x.principal
        and x.id > e.id
   );

create unique index if not exists ux_enderecos_cliente_principal
  on public.enderecos_clientes(cliente_id)
  where principal;

-- Corrige o caso em que a coluna uf ficou como char(1) no banco legado.
do $$ begin
  if exists (
    select 1
      from information_schema.columns
     where table_schema = 'public'
       and table_name = 'enderecos_clientes'
       and column_name = 'uf'
       and character_maximum_length is distinct from 2
  ) then
    alter table public.enderecos_clientes alter column uf type char(2);
  end if;
end $$;


-- ============================================================
-- 3. OFICINAS (dados de impressão / configuração)
-- ============================================================
alter table public.oficinas
  add column if not exists nome_fantasia varchar,
  add column if not exists email varchar,
  add column if not exists whatsapp varchar,
  add column if not exists site varchar,
  add column if not exists logo_url text,
  add column if not exists cep varchar,
  add column if not exists logradouro varchar,
  add column if not exists numero varchar,
  add column if not exists complemento varchar,
  add column if not exists bairro varchar,
  add column if not exists cidade varchar,
  add column if not exists uf char(2),
  add column if not exists horario_funcionamento varchar,
  add column if not exists validade_orcamento_dias integer not null default 7
    check (validade_orcamento_dias >= 1),
  add column if not exists desconto_maximo_percentual numeric(5,2) not null default 0
    check (desconto_maximo_percentual between 0 and 100),
  add column if not exists cor_primaria varchar(7);

-- Normaliza o uf de oficinas para varchar(2). O Hibernate valida o tipo JDBC
-- (String -> VARCHAR); se a coluna existir como char(2)/bpchar no banco legado
-- a aplicação não sobe com ddl-auto=validate.
do $$ begin
  if exists (
    select 1
      from information_schema.columns
     where table_schema = 'public'
       and table_name = 'oficinas'
       and column_name = 'uf'
       and data_type <> 'character varying'
  ) then
    alter table public.oficinas
      alter column uf type varchar(2) using left(btrim(uf::text), 2);
  end if;
end $$;


-- ============================================================
-- 4. USUÁRIOS
-- ============================================================
alter table public.usuarios
  add column if not exists telefone varchar,
  add column if not exists especialidades varchar,
  add column if not exists lider boolean not null default false,
  add column if not exists comissao_percentual numeric(5,2) not null default 0
    check (comissao_percentual between 0 and 100),
  add column if not exists cor_agenda varchar(7),
  add column if not exists foto_url text;

-- Login é por e-mail no Supabase Auth: não pode haver dois usuários com o
-- mesmo e-mail (ignorando maiúsculas/espaços).
-- Se o banco legado já tiver duplicidade, o índice não é criado para não
-- derrubar a migração.
do $$ begin
  if not exists (
    select 1
      from public.usuarios
     group by lower(btrim(email))
    having count(*) > 1
  ) then
    create unique index if not exists ux_usuarios_email
      on public.usuarios(lower(btrim(email)));
  else
    raise notice 'ux_usuarios_email nao criado: existem e-mails duplicados';
  end if;
end $$;


-- ============================================================
-- 5. BOXES
-- ============================================================
alter table public.boxes
  add column if not exists tipo varchar,
  add column if not exists observacoes text;

do $$ begin
  if not exists (
    select 1
      from public.boxes
     group by oficina_id, lower(btrim(nome))
    having count(*) > 1
  ) then
    create unique index if not exists ux_boxes_oficina_nome
      on public.boxes(oficina_id, lower(btrim(nome)));
  else
    raise notice 'ux_boxes_oficina_nome nao criado: existem boxes com nome duplicado na mesma oficina';
  end if;
end $$;


-- ============================================================
-- 6. SERVIÇOS (catálogo)
-- ============================================================
alter table public.servicos
  add column if not exists categoria varchar,
  add column if not exists detalhe text,
  add column if not exists tempo_estimado_horas numeric
    check (tempo_estimado_horas is null or tempo_estimado_horas >= 0),
  add column if not exists garantia_tipo varchar;

do $$ begin
  if not exists (
    select 1
      from public.servicos
     group by oficina_id, lower(btrim(codigo))
    having count(*) > 1
  ) then
    create unique index if not exists ux_servicos_oficina_codigo
      on public.servicos(oficina_id, lower(btrim(codigo)));
  else
    raise notice 'ux_servicos_oficina_codigo nao criado: existem codigos de servico duplicados na mesma oficina';
  end if;
end $$;

-- Peças sugeridas por serviço (o front já usa suggestedParts no catálogo).
create table if not exists public.servico_pecas_sugeridas (
  servico_id bigint not null references public.servicos(id) on delete cascade,
  peca_id    bigint not null references public.pecas(id)    on delete cascade,
  constraint servico_pecas_sugeridas_pk primary key (servico_id, peca_id)
);
create index if not exists ix_servico_pecas_sugeridas_peca
  on public.servico_pecas_sugeridas(peca_id);


-- ============================================================
-- 7. FORNECEDORES E PEÇAS (estoque / catálogo)
-- ============================================================
create table if not exists public.fornecedores (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id) on delete cascade,
  nome varchar not null,
  razao_social varchar,
  cnpj varchar,
  email varchar,
  telefone varchar,
  contato varchar,
  lead_time_dias integer not null default 1
    check (lead_time_dias >= 0),
  endereco text,
  observacoes text,
  ativo boolean not null default true,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists ix_fornecedores_oficina
  on public.fornecedores(oficina_id, nome);
create unique index if not exists ux_fornecedores_oficina_nome
  on public.fornecedores(oficina_id, lower(btrim(nome)));
create unique index if not exists ux_fornecedores_oficina_cnpj
  on public.fornecedores(oficina_id, regexp_replace(cnpj, '\D', '', 'g'))
  where cnpj is not null;

alter table public.pecas
  add column if not exists oem varchar,
  add column if not exists categoria varchar,
  add column if not exists aplicacao text,
  add column if not exists unidade varchar not null default 'un',
  add column if not exists estoque_maximo numeric not null default 0
    check (estoque_maximo >= 0),
  add column if not exists localizacao varchar,
  add column if not exists fornecedor_id bigint
    references public.fornecedores(id) on delete set null,
  add column if not exists observacao text,
  add column if not exists estoque_reservado numeric not null default 0
    check (estoque_reservado >= 0 and estoque_reservado <= estoque_atual),
  add column if not exists codigo_barras varchar,
  add column if not exists garantia_dias integer
    check (garantia_dias is null or garantia_dias >= 0),
  add column if not exists ncm varchar;

-- Saldo disponível = físico - reservado (não precisa ser mantido pelo app).
do $$ begin
  if not exists (
    select 1
      from information_schema.columns
     where table_schema = 'public'
       and table_name = 'pecas'
       and column_name = 'estoque_disponivel'
  ) then
    alter table public.pecas
      add column estoque_disponivel numeric
      generated always as (estoque_atual - estoque_reservado) stored;
  end if;
end $$;

do $$ begin
  if not exists (
    select 1
      from public.pecas
     group by oficina_id, lower(btrim(codigo))
    having count(*) > 1
  ) then
    create unique index if not exists ux_pecas_oficina_codigo
      on public.pecas(oficina_id, lower(btrim(codigo)));
  else
    raise notice 'ux_pecas_oficina_codigo nao criado: existem codigos de peca duplicados na mesma oficina';
  end if;
end $$;

do $$ begin
  if not exists (
    select 1
      from public.pecas
     where codigo_barras is not null
     group by oficina_id, codigo_barras
    having count(*) > 1
  ) then
    create unique index if not exists ux_pecas_oficina_codigo_barras
      on public.pecas(oficina_id, codigo_barras)
      where codigo_barras is not null;
  else
    raise notice 'ux_pecas_oficina_codigo_barras nao criado: existem codigos de barras duplicados na mesma oficina';
  end if;
end $$;
create index if not exists ix_pecas_fornecedor
  on public.pecas(fornecedor_id);
create index if not exists ix_pecas_oficina_categoria
  on public.pecas(oficina_id, categoria);


-- ============================================================
-- 8. CHECKLIST
-- ============================================================
alter table public.checklist_itens
  add column if not exists codigo varchar;

update public.checklist_itens
   set codigo = btrim(
         lower(regexp_replace(btrim(nome), '[^a-zA-Z0-9]+', '_', 'g')),
         '_'
       )
 where codigo is null;

-- Nomes diferentes podem gerar o mesmo código (ex.: "Pneus" e "Pneus!").
-- Resolve as colisões anexando o id antes de criar o índice único.
do $$
declare
  r record;
begin
  for r in
    select id
      from (
        select id,
               row_number() over (
                 partition by oficina_id, codigo order by id
               ) as rn
          from public.checklist_itens
      ) d
     where d.rn > 1
  loop
    update public.checklist_itens
       set codigo = codigo || '_' || r.id
     where id = r.id;
  end loop;
end $$;

alter table public.checklist_itens alter column codigo set not null;

create unique index if not exists ux_checklist_itens_oficina_codigo
  on public.checklist_itens(oficina_id, codigo);

-- Semeia os 12 itens padrão do front para cada oficina existente.
-- Oficinas novas devem receber os mesmos itens no fluxo de cadastro.
insert into public.checklist_itens (oficina_id, codigo, nome)
select o.id, v.codigo, v.nome
  from public.oficinas o
  cross join (values
    ('farois',        'Faróis e luz alta'),
    ('lanternas',     'Lanternas e luz de freio'),
    ('setas',         'Setas e pisca-alerta'),
    ('limpadores',    'Limpadores e esguicho'),
    ('pneus',         'Pneus (desgaste e calibragem)'),
    ('estepe',        'Estepe'),
    ('ferramentas',   'Macaco, chave de roda e triângulo'),
    ('documento',     'Documento do veículo (CRLV)'),
    ('multimidia',    'Rádio / Multimídia'),
    ('ar_condicionado','Ar-condicionado'),
    ('vidros',        'Vidros e travas elétricas'),
    ('painel',        'Painel sem luzes de alerta')
  ) as v(codigo, nome)
on conflict (oficina_id, codigo) do nothing;

alter table public.os_checklist
  add column if not exists criado_em timestamptz not null default now(),
  add column if not exists atualizado_em timestamptz not null default now();

-- Mantém apenas o registro mais recente de cada item na OS (dados legados).
delete from public.os_checklist c
 using public.os_checklist x
 where c.ordem_servico_id = x.ordem_servico_id
   and c.checklist_item_id = x.checklist_item_id
   and c.id < x.id;

create unique index if not exists ux_os_checklist_os_item
  on public.os_checklist(ordem_servico_id, checklist_item_id);

-- Mapeamento do front para o enum do banco (feito no backend):
--   ok            -> CONFORME
--   atencao       -> NAO_CONFORME  (observacao "Atenção")
--   avaria        -> NAO_CONFORME  (observacao "Avaria")
--   nao_aplicavel -> NAO_APLICAVEL


-- ============================================================
-- 9. ORDENS DE SERVIÇO (campos que o front já usa)
-- ============================================================
alter table public.ordens_servico
  add column if not exists tipo varchar,
  add column if not exists canal varchar,
  add column if not exists titulo varchar,
  add column if not exists relato_cliente text,
  add column if not exists observacoes_internas text,
  add column if not exists observacoes_cliente text,
  add column if not exists previsao_entrega timestamptz,
  add column if not exists nivel_combustivel smallint
    check (nivel_combustivel is null or nivel_combustivel between 0 and 4),
  add column if not exists desconto_tipo varchar not null default 'VALOR'
    check (desconto_tipo in ('VALOR','PERCENTUAL')),
  add column if not exists km_saida integer
    check (km_saida is null or km_saida >= 0),
  add column if not exists diagnostico_validado boolean not null default false,
  add column if not exists pronta_em timestamptz,
  add column if not exists entregue_em timestamptz,
  add column if not exists cancelada_em timestamptz,
  add column if not exists motivo_cancelamento text,
  add column if not exists forma_pagamento varchar,
  add column if not exists parcelas smallint not null default 1
    check (parcelas >= 1),
  add column if not exists aprovacao_status varchar
    check (aprovacao_status is null
           or aprovacao_status in ('PENDENTE','APROVADO','RECUSADO')),
  add column if not exists aprovacao_canal varchar,
  add column if not exists aprovacao_enviada_em timestamptz,
  add column if not exists aprovacao_decidida_em timestamptz,
  add column if not exists aprovacao_visualizada_em timestamptz,
  add column if not exists garantia_tipo varchar,
  add column if not exists atualizado_por bigint
    references public.usuarios(id) on delete set null;

create index if not exists ix_ordens_servico_oficina_status
  on public.ordens_servico(oficina_id, status_id, numero desc);
create index if not exists ix_ordens_servico_previsao
  on public.ordens_servico(oficina_id, previsao_entrega);
create index if not exists ix_ordens_servico_tipo
  on public.ordens_servico(oficina_id, tipo);


-- ============================================================
-- 10. ITENS E HISTÓRICO DA OS
-- ============================================================
alter table public.os_servicos
  add column if not exists concluido boolean not null default false,
  add column if not exists concluido_em timestamptz,
  add column if not exists desconto numeric not null default 0
    check (desconto >= 0),
  add column if not exists desconto_tipo varchar not null default 'VALOR'
    check (desconto_tipo in ('VALOR','PERCENTUAL')),
  add column if not exists atualizado_em timestamptz not null default now();

alter table public.os_pecas
  add column if not exists cotada boolean not null default true,
  add column if not exists instalada boolean not null default false,
  add column if not exists instalada_em timestamptz,
  add column if not exists desconto numeric not null default 0
    check (desconto >= 0),
  add column if not exists desconto_tipo varchar not null default 'VALOR'
    check (desconto_tipo in ('VALOR','PERCENTUAL')),
  add column if not exists unidade varchar,
  add column if not exists atualizado_em timestamptz not null default now();

alter table public.os_historico
  add column if not exists tipo_evento varchar not null default 'STATUS',
  add column if not exists metadata jsonb;

create index if not exists ix_os_servicos_mecanico
  on public.os_servicos(mecanico_id);
create index if not exists ix_os_servicos_servico
  on public.os_servicos(servico_id);
create index if not exists ix_os_pecas_peca
  on public.os_pecas(peca_id);

-- Remove vínculos repetidos antes de exigir unicidade (dados legados).
delete from public.os_mecanicos m
 using public.os_mecanicos x
 where m.ordem_servico_id = x.ordem_servico_id
   and m.usuario_id = x.usuario_id
   and m.id > x.id;

create unique index if not exists ux_os_mecanicos_os_usuario
  on public.os_mecanicos(ordem_servico_id, usuario_id);


-- ============================================================
-- 11. STATUS: PRONTA E ENTREGUE DISTINTOS
-- ============================================================
-- PRONTA = pronto para retirada (front: "pronta")
-- ENTREGUE = entregue ao cliente (front: "entregue")
-- CONCLUIDA fica como legado, inativo, para não ser usado em novas OS.
insert into public.status_ordens_servico (nome, ordem, ativo) values
  ('PRONTA',    8, true),
  ('ENTREGUE', 10, true)
on conflict (nome) do update
  set ordem = excluded.ordem,
      ativo = true;

update public.status_ordens_servico
   set ordem = 9, ativo = false
 where nome = 'CONCLUIDA';

update public.status_ordens_servico
   set ordem = 11
 where nome = 'CANCELADA';


-- ============================================================
-- 12. ORÇAMENTOS
-- ============================================================
alter table public.orcamentos
  add column if not exists titulo varchar,
  add column if not exists tipo varchar,
  add column if not exists vendedor_id bigint
    references public.usuarios(id) on delete set null,
  add column if not exists canal_envio varchar,
  add column if not exists enviado_em timestamptz,
  add column if not exists visualizado_em timestamptz,
  add column if not exists decidido_em timestamptz,
  add column if not exists canal_decisao varchar,
  add column if not exists desconto_tipo varchar not null default 'VALOR'
    check (desconto_tipo in ('VALOR','PERCENTUAL')),
  add column if not exists validade_dias integer not null default 7
    check (validade_dias >= 1),
  add column if not exists convertido_em timestamptz,
  add column if not exists atualizado_por bigint
    references public.usuarios(id) on delete set null;

alter table public.orcamento_itens
  add column if not exists unidade varchar,
  add column if not exists desconto numeric not null default 0
    check (desconto >= 0),
  add column if not exists desconto_tipo varchar not null default 'VALOR'
    check (desconto_tipo in ('VALOR','PERCENTUAL')),
  add column if not exists tempo_estimado_horas numeric
    check (tempo_estimado_horas is null or tempo_estimado_horas >= 0),
  add column if not exists mecanico_id bigint
    references public.usuarios(id) on delete set null,
  add column if not exists atualizado_em timestamptz not null default now();

alter table public.orcamento_aprovacoes
  add column if not exists canal varchar,
  add column if not exists usuario_id bigint
    references public.usuarios(id) on delete set null,
  add column if not exists atualizado_em timestamptz not null default now();

-- Um único pedido de aprovação pendente por orçamento.
-- Se o banco legado já tiver mais de um pendente, o índice não é criado para
-- não derrubar a migração; a pendência fica visível para correção manual.
do $$ begin
  if not exists (
    select 1
      from public.orcamento_aprovacoes
     where status = 'PENDENTE'::public.status_aprovacao
     group by orcamento_id
    having count(*) > 1
  ) then
    create unique index if not exists ux_orcamento_aprovacoes_pendente
      on public.orcamento_aprovacoes(orcamento_id)
      where status = 'PENDENTE'::public.status_aprovacao;
  else
    raise notice 'ux_orcamento_aprovacoes_pendente nao criado: existem orcamentos com mais de uma aprovacao PENDENTE';
  end if;
end $$;

create index if not exists ix_orcamentos_cliente
  on public.orcamentos(cliente_id);
create index if not exists ix_orcamentos_veiculo
  on public.orcamentos(veiculo_id);
create index if not exists ix_orcamento_itens_peca
  on public.orcamento_itens(peca_id);
create index if not exists ix_orcamento_itens_servico
  on public.orcamento_itens(servico_id);

-- Numeração de orçamento por oficina (mesmo padrão da OS).
alter table public.oficina_sequencias
  drop constraint if exists oficina_sequencias_entidade_chk;
alter table public.oficina_sequencias
  add constraint oficina_sequencias_entidade_chk
  check (entidade in ('ordem_servico', 'orcamento', 'pedido_compra'));

insert into public.oficina_sequencias as s (oficina_id, entidade, ultimo_numero)
select o.id, 'orcamento', coalesce(max(orc.numero), 0)
  from public.oficinas o
  left join public.orcamentos orc on orc.oficina_id = o.id
 group by o.id
on conflict (oficina_id, entidade) do update
  set ultimo_numero = greatest(s.ultimo_numero, excluded.ultimo_numero);

create or replace function public.fn_proximo_numero_orcamento(p_oficina bigint)
returns integer
language plpgsql
as $$
declare
  v_numero integer;
begin
  insert into public.oficina_sequencias as s (oficina_id, entidade, ultimo_numero)
  values (p_oficina, 'orcamento', 1)
  on conflict (oficina_id, entidade)
  do update set ultimo_numero = s.ultimo_numero + 1
  returning ultimo_numero into v_numero;

  return v_numero;
end;
$$;

create or replace function public.fn_valida_numero_documento()
returns trigger
language plpgsql
as $$
begin
  if new.numero is distinct from old.numero then
    raise exception
      'numero do documento % e imutavel (tentou mudar de % para %)',
      old.id, old.numero, new.numero;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_orcamentos_numero_imutavel on public.orcamentos;
create trigger trg_orcamentos_numero_imutavel
  before update on public.orcamentos
  for each row execute function public.fn_valida_numero_documento();


-- ============================================================
-- 13. VISTORIA DE ENTRADA
-- ============================================================
create table if not exists public.os_vistorias (
  id bigint generated always as identity primary key,
  ordem_servico_id bigint not null
    references public.ordens_servico(id) on delete cascade,
  cliente_ciente boolean not null default false,
  pertences text,
  observacoes text,
  realizada_em timestamptz,
  realizada_por bigint references public.usuarios(id) on delete set null,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create unique index if not exists ux_os_vistorias_os
  on public.os_vistorias(ordem_servico_id);

create table if not exists public.os_vistoria_danos (
  id bigint generated always as identity primary key,
  vistoria_id bigint not null
    references public.os_vistorias(id) on delete cascade,
  area varchar not null,
  tipo varchar not null,
  observacao text,
  criado_em timestamptz not null default now()
);
create unique index if not exists ux_os_vistoria_danos_area_tipo
  on public.os_vistoria_danos(vistoria_id, area, tipo);
create index if not exists ix_os_vistoria_danos_vistoria
  on public.os_vistoria_danos(vistoria_id);

create table if not exists public.os_vistoria_fotos (
  id bigint generated always as identity primary key,
  vistoria_id bigint not null
    references public.os_vistorias(id) on delete cascade,
  url text not null,
  storage_path text,
  legenda varchar,
  criado_em timestamptz not null default now()
);
create index if not exists ix_os_vistoria_fotos_vistoria
  on public.os_vistoria_fotos(vistoria_id);


-- ============================================================
-- 14. ESTOQUE: PEDIDOS DE COMPRA E MOVIMENTAÇÕES
-- ============================================================
create table if not exists public.pedidos_compra (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id) on delete cascade,
  numero integer not null,
  fornecedor_id bigint not null references public.fornecedores(id),
  status varchar not null default 'RASCUNHO'
    check (status in ('RASCUNHO','ENVIADO','RECEBIDO','CANCELADO')),
  data_pedido date not null default current_date,
  previsao_entrega date,
  recebido_em timestamptz,
  nota_fiscal varchar,
  observacoes text,
  criado_por bigint references public.usuarios(id) on delete set null,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create unique index if not exists ux_pedidos_compra_oficina_numero
  on public.pedidos_compra(oficina_id, numero);
create index if not exists ix_pedidos_compra_oficina_status
  on public.pedidos_compra(oficina_id, status, data_pedido desc);
create index if not exists ix_pedidos_compra_fornecedor
  on public.pedidos_compra(fornecedor_id);

create table if not exists public.pedido_compra_itens (
  id bigint generated always as identity primary key,
  pedido_compra_id bigint not null
    references public.pedidos_compra(id) on delete cascade,
  peca_id bigint references public.pecas(id) on delete set null,
  descricao varchar not null,
  quantidade numeric not null check (quantidade > 0),
  custo_unitario numeric not null default 0 check (custo_unitario >= 0),
  quantidade_recebida numeric not null default 0
    check (quantidade_recebida >= 0),
  criado_em timestamptz not null default now(),
  constraint pedido_compra_itens_recebido_chk
    check (quantidade_recebida <= quantidade)
);
create index if not exists ix_pedido_compra_itens_pedido
  on public.pedido_compra_itens(pedido_compra_id);
create index if not exists ix_pedido_compra_itens_peca
  on public.pedido_compra_itens(peca_id);

insert into public.oficina_sequencias as s (oficina_id, entidade, ultimo_numero)
select o.id, 'pedido_compra', 0
  from public.oficinas o
on conflict (oficina_id, entidade) do nothing;

create or replace function public.fn_proximo_numero_pedido_compra(p_oficina bigint)
returns integer
language plpgsql
as $$
declare
  v_numero integer;
begin
  insert into public.oficina_sequencias as s (oficina_id, entidade, ultimo_numero)
  values (p_oficina, 'pedido_compra', 1)
  on conflict (oficina_id, entidade)
  do update set ultimo_numero = s.ultimo_numero + 1
  returning ultimo_numero into v_numero;

  return v_numero;
end;
$$;

drop trigger if exists trg_pedidos_compra_numero_imutavel on public.pedidos_compra;
create trigger trg_pedidos_compra_numero_imutavel
  before update on public.pedidos_compra
  for each row execute function public.fn_valida_numero_documento();

alter table public.estoque_movimentacoes
  add column if not exists direcao smallint
    check (direcao in (-1, 1)),
  add column if not exists saldo_anterior numeric,
  add column if not exists saldo_posterior numeric,
  add column if not exists custo_unitario numeric
    check (custo_unitario is null or custo_unitario >= 0),
  add column if not exists documento varchar,
  add column if not exists pedido_compra_id bigint
    references public.pedidos_compra(id) on delete set null,
  add column if not exists fornecedor_id bigint
    references public.fornecedores(id) on delete set null;

-- Preenche a direção das movimentações antigas pelo rótulo do enum.
-- (As antigas não são reaplicadas ao saldo: pecas.estoque_atual é a verdade.)
update public.estoque_movimentacoes
   set direcao = case
     when tipo::text ilike '%SAIDA%' then -1
     when tipo::text ilike '%ENTRADA%' or tipo::text ilike '%DEVOLU%' then 1
     else null
   end
 where direcao is null;

create or replace function public.fn_aplica_movimentacao_estoque()
returns trigger
language plpgsql
as $$
declare
  v_saldo numeric;
  v_oficina bigint;
  v_novo_saldo numeric;
begin
  if new.direcao is null then
    raise exception
      'direcao da movimentacao de estoque e obrigatoria (1 entrada, -1 saida)';
  end if;

  select p.estoque_atual, p.oficina_id
    into v_saldo, v_oficina
    from public.pecas p
   where p.id = new.peca_id
   for update;

  if not found then
    raise exception 'peca % nao encontrada', new.peca_id;
  end if;

  if v_oficina is distinct from new.oficina_id then
    raise exception
      'peca % pertence a oficina %, nao a %',
      new.peca_id, v_oficina, new.oficina_id;
  end if;

  v_novo_saldo := v_saldo + new.direcao * new.quantidade;

  if v_novo_saldo < 0 then
    raise exception
      'estoque insuficiente da peca %: saldo %, movimento %',
      new.peca_id, v_saldo, new.direcao * new.quantidade;
  end if;

  new.saldo_anterior := v_saldo;
  new.saldo_posterior := v_novo_saldo;

  update public.pecas
     set estoque_atual = v_novo_saldo
   where id = new.peca_id;

  return new;
end;
$$;

drop trigger if exists trg_estoque_movimentacoes_aplica
  on public.estoque_movimentacoes;
create trigger trg_estoque_movimentacoes_aplica
  before insert on public.estoque_movimentacoes
  for each row execute function public.fn_aplica_movimentacao_estoque();

create index if not exists ix_estoque_mov_pedido
  on public.estoque_movimentacoes(pedido_compra_id);
create index if not exists ix_estoque_mov_fornecedor
  on public.estoque_movimentacoes(fornecedor_id);


-- ============================================================
-- 15. FINANCEIRO
-- ============================================================
create table if not exists public.pagamentos (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id) on delete cascade,
  ordem_servico_id bigint
    references public.ordens_servico(id) on delete set null,
  orcamento_id bigint
    references public.orcamentos(id) on delete set null,
  cliente_id bigint
    references public.clientes(id) on delete set null,
  descricao varchar,
  valor numeric(12,2) not null check (valor > 0),
  forma_pagamento varchar,
  parcelas smallint not null default 1 check (parcelas >= 1),
  status varchar not null default 'PENDENTE'
    check (status in ('PENDENTE','PAGO','CANCELADO','ESTORNADO')),
  vencimento date,
  pago_em timestamptz,
  observacoes text,
  criado_por bigint references public.usuarios(id) on delete set null,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists ix_pagamentos_oficina_status
  on public.pagamentos(oficina_id, status, vencimento);
create index if not exists ix_pagamentos_os
  on public.pagamentos(ordem_servico_id);
create index if not exists ix_pagamentos_orcamento
  on public.pagamentos(orcamento_id);
create index if not exists ix_pagamentos_cliente
  on public.pagamentos(cliente_id);

create table if not exists public.despesas (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id) on delete cascade,
  fornecedor_id bigint
    references public.fornecedores(id) on delete set null,
  descricao varchar not null,
  categoria varchar,
  valor numeric(12,2) not null check (valor > 0),
  status varchar not null default 'PENDENTE'
    check (status in ('PENDENTE','PAGO','CANCELADO')),
  vencimento date,
  pago_em timestamptz,
  observacoes text,
  criado_por bigint references public.usuarios(id) on delete set null,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists ix_despesas_oficina_status
  on public.despesas(oficina_id, status, vencimento);
create index if not exists ix_despesas_fornecedor
  on public.despesas(fornecedor_id);


-- ============================================================
-- 16. AGENDA
-- ============================================================
create table if not exists public.agendamentos (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id) on delete cascade,
  cliente_id bigint references public.clientes(id) on delete set null,
  veiculo_id bigint references public.veiculos(id) on delete set null,
  servico_id bigint references public.servicos(id) on delete set null,
  mecanico_id bigint references public.usuarios(id) on delete set null,
  box_id bigint references public.boxes(id) on delete set null,
  ordem_servico_id bigint
    references public.ordens_servico(id) on delete set null,
  titulo varchar not null,
  descricao text,
  inicio timestamptz not null,
  fim timestamptz not null,
  status varchar not null default 'AGENDADO'
    check (status in ('AGENDADO','CONFIRMADO','EM_ANDAMENTO',
                      'CONCLUIDO','CANCELADO','NAO_COMPARECEU')),
  criado_por bigint references public.usuarios(id) on delete set null,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  constraint agendamentos_periodo_chk check (fim > inicio)
);
create index if not exists ix_agendamentos_oficina_inicio
  on public.agendamentos(oficina_id, inicio);
create index if not exists ix_agendamentos_mecanico_inicio
  on public.agendamentos(mecanico_id, inicio);
create index if not exists ix_agendamentos_veiculo
  on public.agendamentos(veiculo_id);


-- ============================================================
-- 17. NOTIFICAÇÕES
-- ============================================================
create table if not exists public.notificacoes (
  id bigint generated always as identity primary key,
  oficina_id bigint not null references public.oficinas(id) on delete cascade,
  usuario_id bigint references public.usuarios(id) on delete cascade,
  tipo varchar,
  titulo varchar not null,
  mensagem text,
  link varchar,
  lida boolean not null default false,
  criado_em timestamptz not null default now(),
  lida_em timestamptz
);
create index if not exists ix_notificacoes_oficina_lida
  on public.notificacoes(oficina_id, lida, criado_em desc);
create index if not exists ix_notificacoes_usuario
  on public.notificacoes(usuario_id, lida, criado_em desc);


-- ============================================================
-- 18. TRIGGERS DE atualizado_em (todas as tabelas que têm a coluna)
-- ============================================================
do $$
declare
  r record;
begin
  for r in
    select c.table_name
      from information_schema.columns c
      join information_schema.tables t
        on t.table_schema = c.table_schema
       and t.table_name = c.table_name
     where c.table_schema = 'public'
       and c.column_name = 'atualizado_em'
       and t.table_type = 'BASE TABLE'
  loop
    execute format(
      'drop trigger if exists trg_%I_updated_at on public.%I',
      r.table_name, r.table_name
    );
    execute format(
      'create trigger trg_%I_updated_at before update on public.%I
       for each row execute function public.fn_atualiza_atualizado_em()',
      r.table_name, r.table_name
    );
  end loop;
end $$;


-- ============================================================
-- 19. RLS (Supabase)
-- ============================================================
-- O backend acessa o banco pelo papel dono das tabelas (postgres), que
-- ignora RLS. O front NÃO acessa as tabelas direto: ele usa a API Spring.
-- Portanto:
--   * RLS fica ligado em todas as tabelas de public;
--   * é criada uma policy permissiva apenas para o papel da API;
--   * anon/authenticated não recebem nenhum grant.
-- Se um dia o front for falar direto com o Supabase, crie policies por
-- oficina usando usuarios.auth_user_id = auth.uid().
do $$
declare
  r record;
begin
  for r in
    select tablename
      from pg_tables
     where schemaname = 'public'
       -- NÃO tocar na tabela de histórico do Flyway: ela já está travada
       -- pela conexão do advisory lock durante a migração, e um ALTER TABLE
       -- aqui causaria deadlock (a migração nunca terminaria).
       and tablename <> 'flyway_schema_history'
  loop
    execute format(
      'alter table public.%I enable row level security',
      r.tablename
    );

    execute format(
      'drop policy if exists api_role_full_access on public.%I',
      r.tablename
    );

    execute format(
      'create policy api_role_full_access on public.%I
       for all to %I using (true) with check (true)',
      r.tablename, current_user
    );
  end loop;
end $$;

do $$ begin
  if exists (select 1 from pg_roles where rolname = 'anon') then
    execute 'revoke all on all tables in schema public from anon';
    execute 'revoke all on all sequences in schema public from anon';
    execute 'revoke all on all functions in schema public from anon';
    execute 'revoke create on schema public from anon';
    execute 'alter default privileges in schema public revoke all on tables from anon';
    execute 'alter default privileges in schema public revoke all on sequences from anon';
    execute 'alter default privileges in schema public revoke all on functions from anon';
  end if;

  if exists (select 1 from pg_roles where rolname = 'authenticated') then
    execute 'revoke all on all tables in schema public from authenticated';
    execute 'revoke all on all sequences in schema public from authenticated';
    execute 'revoke all on all functions in schema public from authenticated';
    execute 'revoke create on schema public from authenticated';
    execute 'alter default privileges in schema public revoke all on tables from authenticated';
    execute 'alter default privileges in schema public revoke all on sequences from authenticated';
    execute 'alter default privileges in schema public revoke all on functions from authenticated';
  end if;
end $$;


-- ============================================================
-- 20. COMENTÁRIOS
-- ============================================================
comment on table public.fornecedores is
  'Fornecedores de peças por oficina.';
comment on table public.pedidos_compra is
  'Pedido de compra de peças; numero sequencial por oficina.';
comment on table public.os_vistorias is
  'Vistoria de entrada da OS: dados gerais, danos e fotos.';
comment on table public.pagamentos is
  'Contas a receber (OS, orçamento ou avulso).';
comment on table public.despesas is
  'Contas a pagar da oficina.';
comment on table public.agendamentos is
  'Agenda de serviços por oficina, mecânico e box.';
comment on table public.notificacoes is
  'Notificações por oficina/usuário.';
comment on column public.pecas.estoque_disponivel is
  'Saldo físico menos reservado (coluna gerada).';
comment on column public.veiculos.oficina_id is
  'Oficina dona do veículo; preenchida a partir do cliente.';
