-- ============================================================
-- V3__numeracao_sequencial_os.sql
-- Numero da OS como identificador de negocio: sequencial por
-- oficina, comecando em 1, e imutavel.
-- ============================================================

-- ---------- 1. Contador por oficina ----------
-- Uma linha por (oficina, entidade) com o ultimo numero
-- consumido. O numero e queimado: se a OS for excluida, a
-- proxima continua depois dela em vez de reusar o numero.
-- Reusar numero de OS e perigoso porque a OS pode estar
-- impressa em documento que o cliente ja tem em maos.
create table if not exists public.oficina_sequencias (
  oficina_id    bigint  not null references public.oficinas (id) on delete cascade,
  entidade      text    not null,
  ultimo_numero integer not null default 0,
  constraint oficina_sequencias_pk primary key (oficina_id, entidade),
  constraint oficina_sequencias_entidade_chk
    check (entidade in ('ordem_servico', 'orcamento')),
  constraint oficina_sequencias_ultimo_chk check (ultimo_numero >= 0)
);

comment on table public.oficina_sequencias is
  'Ultimo numero de documento consumido por oficina e entidade. '
  'O numero nunca e reusado, mesmo apos a exclusao do documento.';

-- ---------- 2. Alinhar o contador com o que ja existe ----------
-- Sem isso, uma oficina que ja tem OS 1..12 voltaria a gerar 1.
insert into public.oficina_sequencias (oficina_id, entidade, ultimo_numero)
select o.id, 'ordem_servico', coalesce(max(os.numero), 0)
  from public.oficinas o
  left join public.ordens_servico os on os.oficina_id = o.id
 group by o.id
on conflict (oficina_id, entidade) do update
  set ultimo_numero = excluded.ultimo_numero;

-- ---------- 3. Alocacao atomica ----------
-- on conflict do update returning entrega numeros diferentes
-- para transacoes concorrentes, sem lock explicito e sem
-- max(numero), que sofre com duas insercoes simultaneas.
create or replace function public.fn_proximo_numero_ordem_servico(p_oficina bigint)
returns integer
language plpgsql
as $$
declare
  v_numero integer;
begin
  insert into public.oficina_sequencias as s (oficina_id, entidade, ultimo_numero)
  values (p_oficina, 'ordem_servico', 1)
  on conflict (oficina_id, entidade)
  do update set ultimo_numero = s.ultimo_numero + 1
  returning ultimo_numero into v_numero;

  return v_numero;
end;
$$;

-- ---------- 4. Numero imutavel ----------
-- O numero e o identificador de negocio da OS. Ele nao pode
-- mudar junto com o status, senao o documento que o cliente
-- tem na mao deixa de bater com o sistema.
create or replace function public.fn_valida_numero_ordem_servico()
returns trigger
language plpgsql
as $$
begin
  if new.numero is distinct from old.numero then
    raise exception
      'numero da OS % e imutavel (tentou mudar de % para %)',
      old.id, old.numero, new.numero;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_ordens_servico_numero_imutavel on public.ordens_servico;

create trigger trg_ordens_servico_numero_imutavel
  before update on public.ordens_servico
  for each row
  execute function public.fn_valida_numero_ordem_servico();