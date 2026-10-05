-- ============================================================
-- V2__corrigir_baseline.sql
-- Correcoes sobre o schema pre-existente, apos a V1 ter sido
-- aplicada como baseline.
-- ============================================================

-- ---------- 1. Nomes de coluna padronizados ----------
-- O schema original misturava criado_em/criada_em e
-- atualizado_em/atualizada_em. Padroniza em criado_em e
-- atualizado_em, que e o nome usado pela maioria das tabelas.
do $$ begin
  alter table public.oficinas rename column criada_em to criado_em;
exception when undefined_column then null; end $$;

do $$ begin
  alter table public.ordens_servico rename column criada_em to criado_em;
exception when undefined_column then null; end $$;

do $$ begin
  alter table public.ordens_servico rename column atualizada_em to atualizado_em;
exception when undefined_column then null; end $$;

-- oficinas nunca teve coluna de atualizacao (o trigger original
-- referenciava uma coluna inexistente e quebrava todo UPDATE).
alter table public.oficinas
  add column if not exists atualizado_em timestamptz not null default now();

-- ---------- 2. Uma unica funcao de trigger, um trigger por tabela ----------
-- remove os dois conjuntos de triggers antes de remover as funcoes
do $$
declare
  t text;
begin
  foreach t in array array[
    'oficinas','clientes','veiculos','usuarios','servicos',
    'pecas','ordens_servico','orcamentos'
  ] loop
    execute format('drop trigger if exists trg_%I_atualizada_em on public.%I', t, t);
    execute format('drop trigger if exists trg_%I_updated_at on public.%I', t, t);
  end loop;
end $$;

drop function if exists public.fn_atualiza_atualizada_em();
drop function if exists public.atualizar_updated_at();

create or replace function public.fn_atualiza_atualizado_em()
returns trigger
language plpgsql
as $$
begin
  new.atualizado_em := now();
  return new;
end;
$$;

do $$
declare
  t text;
begin
  foreach t in array array[
    'oficinas','clientes','veiculos','usuarios','servicos',
    'pecas','ordens_servico','orcamentos'
  ] loop
    execute format(
      'create trigger trg_%I_updated_at before update on public.%I
       for each row execute function public.fn_atualiza_atualizado_em()', t, t);
  end loop;
end $$;

-- ---------- 3. Status da OS: um nome por fluxo, sempre com underscore ----------
-- remove as versoes com espaco que a V1 inseriu
delete from public.status_ordens_servico
where nome in ('EM DIAGNOSTICO','AGUARDANDO APROVACAO','AGUARDANDO PECA',
               'EM EXECUCAO','EM TESTE');

insert into public.status_ordens_servico (nome, ordem, ativo) values
  ('ABERTA',               1, true),
  ('EM_ORCAMENTO',         2, true),
  ('EM_DIAGNOSTICO',       3, true),
  ('AGUARDANDO_APROVACAO', 4, true),
  ('AGUARDANDO_PECA',      5, true),
  ('EM_EXECUCAO',          6, true),
  ('EM_TESTE',             7, true),
  ('CONCLUIDA',            8, true),
  ('CANCELADA',            9, true)
on conflict (nome) do update set ordem = excluded.ordem, ativo = excluded.ativo;

-- ---------- 4. Unicidade do numero por oficina ----------
-- declarados inline na V1, dentro de create table if not exists
-- que foi ignorado: nunca chegaram a existir.
create unique index if not exists ux_ordens_servico_oficina_numero
  on public.ordens_servico (oficina_id, numero);

create unique index if not exists ux_orcamentos_oficina_numero
  on public.orcamentos (oficina_id, numero);

-- ---------- 5. Enum orfao criado pela V1 ----------
drop type if exists public.tipo_orcamento_item;

-- ---------- 6. Views ----------
-- existiam no banco e nao estavam em nenhuma migracao.
-- Conferir contra o banco: select viewname, definition from pg_views
-- where schemaname = 'public';
create or replace view public.vw_orcamento_resumo as
select o.id,
       o.oficina_id,
       o.numero,
       o.cliente_id,
       o.veiculo_id,
       o.status,
       cast(coalesce(sum(i.quantidade * i.valor_unitario), 0) as numeric(12,2)) as subtotal,
       o.desconto,
       cast(coalesce(sum(i.quantidade * i.valor_unitario), 0) - o.desconto as numeric(12,2)) as total
  from orcamentos o
  left join orcamento_itens i on i.orcamento_id = o.id
 group by o.id, o.oficina_id, o.numero, o.cliente_id, o.veiculo_id, o.status, o.desconto;

create or replace view public.vw_ordem_servico_resumo as
select os.id,
       os.oficina_id,
       os.numero,
       os.cliente_id,
       os.veiculo_id,
       os.status_id,
       os.prioridade,
       cast(coalesce(s.total_servicos, 0) as numeric(12,2)) as total_servicos,
       cast(coalesce(p.total_pecas, 0) as numeric(12,2)) as total_pecas,
       cast(coalesce(s.total_servicos, 0) + coalesce(p.total_pecas, 0) - os.desconto
            as numeric(12,2)) as total
  from ordens_servico os
  left join (
        select ordem_servico_id, sum(quantidade * valor_unitario) as total_servicos
          from os_servicos group by ordem_servico_id
       ) s on s.ordem_servico_id = os.id
  left join (
        select ordem_servico_id, sum(quantidade * valor_unitario) as total_pecas
          from os_pecas group by ordem_servico_id
       ) p on p.ordem_servico_id = os.id;
