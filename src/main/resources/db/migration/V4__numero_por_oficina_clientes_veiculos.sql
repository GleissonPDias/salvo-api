-- ============================================================
-- V4__numero_por_oficina_clientes_veiculos.sql
--
-- Numeração sequencial por oficina para clientes e veículos.
--
-- Princípios:
--   * idempotente;
--   * aditiva: nenhum dado existente é apagado;
--   * não altera V1 a V3.
-- ============================================================

alter table public.clientes
  add column if not exists numero integer not null default 0;

alter table public.veiculos
  add column if not exists numero integer not null default 0;

update public.clientes c
   set numero = sub.rn
  from (
        select id,
               row_number() over (partition by oficina_id order by criado_em asc, id asc) as rn
          from public.clientes
       ) sub
 where c.id = sub.id
   and c.numero = 0;

update public.veiculos v
   set numero = sub.rn
  from (
        select id,
               row_number() over (partition by oficina_id order by criado_em asc, id asc) as rn
          from public.veiculos
       ) sub
 where v.id = sub.id
   and v.numero = 0;

create unique index if not exists ux_clientes_oficina_numero
  on public.clientes (oficina_id, numero);

create unique index if not exists ux_veiculos_oficina_numero
  on public.veiculos (oficina_id, numero);
