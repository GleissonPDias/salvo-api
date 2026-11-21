-- ============================================================
-- V1__baseline.sql
--
-- Schema consolidado do banco (estado atual de producao).
-- Substitui V1 a V9. Gerado a partir de pg_dump --schema-only do Supabase.
--
-- Para bancos novos. Migracoes incrementais voltam a partir de V2.
-- Nao editar: toda mudanca futura entra em uma nova versao (V2, V3...).
-- ============================================================

--
-- PostgreSQL database dump
--


-- Dumped from database version 17.11
-- Dumped by pg_dump version 17.12 (Debian 17.12-0+deb13u1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--



--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: prioridade_os; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.prioridade_os AS ENUM (
    'BAIXA',
    'NORMAL',
    'ALTA',
    'URGENTE'
);


--
-- Name: status_aprovacao; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.status_aprovacao AS ENUM (
    'PENDENTE',
    'APROVADO',
    'REJEITADO'
);


--
-- Name: status_checklist; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.status_checklist AS ENUM (
    'OK',
    'ATENCAO',
    'PROBLEMA',
    'NAO_VERIFICADO'
);


--
-- Name: status_orcamento; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.status_orcamento AS ENUM (
    'RASCUNHO',
    'AGUARDANDO_RESPOSTA',
    'APROVADO',
    'REJEITADO',
    'VENCIDO'
);


--
-- Name: tipo_item_orcamento; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_item_orcamento AS ENUM (
    'PECA',
    'SERVICO'
);


--
-- Name: tipo_movimentacao_estoque; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_movimentacao_estoque AS ENUM (
    'ENTRADA',
    'SAIDA',
    'AJUSTE'
);


--
-- Name: fn_aplica_movimentacao_estoque(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_aplica_movimentacao_estoque() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: fn_atualiza_atualizado_em(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_atualiza_atualizado_em() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  new.atualizado_em := now();
  return new;
end;
$$;


--
-- Name: fn_preenche_veiculo_oficina(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_preenche_veiculo_oficina() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: fn_proximo_numero_orcamento(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proximo_numero_orcamento(p_oficina bigint) RETURNS integer
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: fn_proximo_numero_ordem_servico(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proximo_numero_ordem_servico(p_oficina bigint) RETURNS integer
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: fn_proximo_numero_pedido_compra(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proximo_numero_pedido_compra(p_oficina bigint) RETURNS integer
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: fn_valida_numero_documento(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_valida_numero_documento() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if new.numero is distinct from old.numero then
    raise exception
      'numero do documento % e imutavel (tentou mudar de % para %)',
      old.id, old.numero, new.numero;
  end if;
  return new;
end;
$$;


--
-- Name: fn_valida_numero_ordem_servico(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_valida_numero_ordem_servico() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if new.numero is distinct from old.numero then
    raise exception
      'numero da OS % e imutavel (tentou mudar de % para %)',
      old.id, old.numero, new.numero;
  end if;
  return new;
end;
$$;


--
-- Name: rls_auto_enable(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.rls_auto_enable() RETURNS event_trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'pg_catalog'
    AS $$
DECLARE
  cmd record;
BEGIN
  FOR cmd IN
    SELECT *
    FROM pg_event_trigger_ddl_commands()
    WHERE command_tag IN ('CREATE TABLE', 'CREATE TABLE AS', 'SELECT INTO')
      AND object_type IN ('table','partitioned table')
  LOOP
     IF cmd.schema_name IS NOT NULL AND cmd.schema_name IN ('public') AND cmd.schema_name NOT IN ('pg_catalog','information_schema') AND cmd.schema_name NOT LIKE 'pg_toast%' AND cmd.schema_name NOT LIKE 'pg_temp%' THEN
      BEGIN
        EXECUTE format('alter table if exists %s enable row level security', cmd.object_identity);
        RAISE LOG 'rls_auto_enable: enabled RLS on %', cmd.object_identity;
      EXCEPTION
        WHEN OTHERS THEN
          RAISE LOG 'rls_auto_enable: failed to enable RLS on %', cmd.object_identity;
      END;
     ELSE
        RAISE LOG 'rls_auto_enable: skip % (either system schema or not in enforced list: %.)', cmd.object_identity, cmd.schema_name;
     END IF;
  END LOOP;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: agendamentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.agendamentos (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    cliente_id bigint,
    veiculo_id bigint,
    servico_id bigint,
    mecanico_id bigint,
    box_id bigint,
    ordem_servico_id bigint,
    titulo character varying NOT NULL,
    descricao text,
    inicio timestamp with time zone NOT NULL,
    fim timestamp with time zone NOT NULL,
    status character varying DEFAULT 'AGENDADO'::character varying NOT NULL,
    criado_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT agendamentos_periodo_chk CHECK ((fim > inicio)),
    CONSTRAINT agendamentos_status_check CHECK (((status)::text = ANY ((ARRAY['AGENDADO'::character varying, 'CONFIRMADO'::character varying, 'EM_ANDAMENTO'::character varying, 'CONCLUIDO'::character varying, 'CANCELADO'::character varying, 'NAO_COMPARECEU'::character varying])::text[])))
);


--
-- Name: TABLE agendamentos; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.agendamentos IS 'Agenda de serviços por oficina, mecânico e box.';


--
-- Name: agendamentos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.agendamentos ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.agendamentos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auditoria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    usuario_id bigint,
    usuario_nome character varying NOT NULL,
    acao character varying(30) NOT NULL,
    modulo character varying(30),
    entidade character varying(60),
    entidade_id character varying(60),
    metodo character varying(10) NOT NULL,
    caminho character varying(300) NOT NULL,
    status_http integer,
    resumo text,
    criado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: auditoria_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auditoria ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.auditoria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: boxes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.boxes (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    nome character varying(50) NOT NULL,
    descricao character varying(150),
    ativo boolean DEFAULT true NOT NULL,
    tipo character varying,
    observacoes text
);


--
-- Name: boxes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.boxes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.boxes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: checklist_itens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.checklist_itens (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    nome character varying(150) NOT NULL,
    descricao character varying(255),
    ativo boolean DEFAULT true NOT NULL,
    codigo character varying NOT NULL
);


--
-- Name: checklist_itens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.checklist_itens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.checklist_itens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: clientes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clientes (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    nome character varying(150) NOT NULL,
    cpf_cnpj character varying(18) NOT NULL,
    email character varying(150),
    telefone character varying(20) NOT NULL,
    telefone_secundario character varying(20),
    classificacao character varying(50),
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    whatsapp boolean DEFAULT false NOT NULL,
    aceita_comunicacoes boolean DEFAULT false NOT NULL,
    observacoes text
);


--
-- Name: clientes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.clientes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.clientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: despesas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.despesas (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    fornecedor_id bigint,
    descricao character varying NOT NULL,
    categoria character varying,
    valor numeric(12,2) NOT NULL,
    status character varying DEFAULT 'PENDENTE'::character varying NOT NULL,
    vencimento date,
    pago_em timestamp with time zone,
    observacoes text,
    criado_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT despesas_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'PAGO'::character varying, 'CANCELADO'::character varying])::text[]))),
    CONSTRAINT despesas_valor_check CHECK ((valor > (0)::numeric))
);


--
-- Name: TABLE despesas; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.despesas IS 'Contas a pagar da oficina.';


--
-- Name: despesas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.despesas ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.despesas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: enderecos_clientes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enderecos_clientes (
    id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    cep character varying(9) NOT NULL,
    logradouro character varying(150) NOT NULL,
    numero character varying(20) NOT NULL,
    complemento character varying(100),
    bairro character varying(100) NOT NULL,
    cidade character varying(100) NOT NULL,
    uf character(2) NOT NULL,
    principal boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: enderecos_clientes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.enderecos_clientes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.enderecos_clientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estoque_movimentacoes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estoque_movimentacoes (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    peca_id bigint NOT NULL,
    tipo public.tipo_movimentacao_estoque NOT NULL,
    quantidade numeric(12,3) NOT NULL,
    ordem_servico_id bigint,
    usuario_id bigint,
    observacao character varying(255),
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    direcao smallint,
    saldo_anterior numeric,
    saldo_posterior numeric,
    custo_unitario numeric,
    documento character varying,
    pedido_compra_id bigint,
    fornecedor_id bigint,
    CONSTRAINT estoque_movimentacoes_custo_unitario_check CHECK (((custo_unitario IS NULL) OR (custo_unitario >= (0)::numeric))),
    CONSTRAINT estoque_movimentacoes_direcao_check CHECK ((direcao = ANY (ARRAY['-1'::integer, 1]))),
    CONSTRAINT estoque_movimentacoes_quantidade_check CHECK ((quantidade > (0)::numeric))
);


--
-- Name: estoque_movimentacoes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.estoque_movimentacoes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.estoque_movimentacoes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: fornecedores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fornecedores (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    nome character varying NOT NULL,
    razao_social character varying,
    cnpj character varying,
    email character varying,
    telefone character varying,
    contato character varying,
    lead_time_dias integer DEFAULT 1 NOT NULL,
    endereco text,
    observacoes text,
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT fornecedores_lead_time_dias_check CHECK ((lead_time_dias >= 0))
);


--
-- Name: TABLE fornecedores; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.fornecedores IS 'Fornecedores de peças por oficina.';


--
-- Name: fornecedores_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.fornecedores ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.fornecedores_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: notificacoes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notificacoes (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    usuario_id bigint,
    tipo character varying,
    titulo character varying NOT NULL,
    mensagem text,
    link character varying,
    lida boolean DEFAULT false NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    lida_em timestamp with time zone
);


--
-- Name: TABLE notificacoes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.notificacoes IS 'Notificações por oficina/usuário.';


--
-- Name: notificacoes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.notificacoes ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.notificacoes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: oficina_sequencias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oficina_sequencias (
    oficina_id bigint NOT NULL,
    entidade text NOT NULL,
    ultimo_numero integer DEFAULT 0 NOT NULL,
    CONSTRAINT oficina_sequencias_entidade_chk CHECK ((entidade = ANY (ARRAY['ordem_servico'::text, 'orcamento'::text, 'pedido_compra'::text]))),
    CONSTRAINT oficina_sequencias_ultimo_chk CHECK ((ultimo_numero >= 0))
);


--
-- Name: TABLE oficina_sequencias; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.oficina_sequencias IS 'Ultimo numero de documento consumido por oficina e entidade. O numero nunca e reusado, mesmo apos a exclusao do documento.';


--
-- Name: oficinas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oficinas (
    id bigint NOT NULL,
    nome character varying(150) NOT NULL,
    cnpj character varying(18),
    telefone character varying(20),
    ativa boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    nome_fantasia character varying,
    email character varying,
    whatsapp character varying,
    site character varying,
    logo_url text,
    cep character varying,
    logradouro character varying,
    numero character varying,
    complemento character varying,
    bairro character varying,
    cidade character varying,
    uf character varying(2),
    horario_funcionamento character varying,
    validade_orcamento_dias integer DEFAULT 7 NOT NULL,
    desconto_maximo_percentual numeric(5,2) DEFAULT 0 NOT NULL,
    cor_primaria character varying(7),
    CONSTRAINT oficinas_desconto_maximo_percentual_check CHECK (((desconto_maximo_percentual >= (0)::numeric) AND (desconto_maximo_percentual <= (100)::numeric))),
    CONSTRAINT oficinas_validade_orcamento_dias_check CHECK ((validade_orcamento_dias >= 1))
);


--
-- Name: oficinas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.oficinas ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.oficinas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: orcamento_aprovacoes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamento_aprovacoes (
    id bigint NOT NULL,
    orcamento_id bigint NOT NULL,
    status public.status_aprovacao DEFAULT 'PENDENTE'::public.status_aprovacao NOT NULL,
    token uuid DEFAULT gen_random_uuid() NOT NULL,
    aprovado_em timestamp with time zone,
    motivo text,
    ip inet,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    canal character varying,
    usuario_id bigint,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: orcamento_aprovacoes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.orcamento_aprovacoes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.orcamento_aprovacoes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: orcamento_itens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamento_itens (
    id bigint NOT NULL,
    orcamento_id bigint NOT NULL,
    tipo character varying(10) NOT NULL,
    peca_id bigint,
    servico_id bigint,
    codigo character varying(50),
    descricao character varying(255) NOT NULL,
    quantidade numeric(12,3) NOT NULL,
    valor_unitario numeric(12,2) NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    unidade character varying,
    desconto numeric DEFAULT 0 NOT NULL,
    desconto_tipo character varying DEFAULT 'VALOR'::character varying NOT NULL,
    tempo_estimado_horas numeric,
    mecanico_id bigint,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT orcamento_itens_desconto_check CHECK ((desconto >= (0)::numeric)),
    CONSTRAINT orcamento_itens_desconto_tipo_check CHECK (((desconto_tipo)::text = ANY ((ARRAY['VALOR'::character varying, 'PERCENTUAL'::character varying])::text[]))),
    CONSTRAINT orcamento_itens_quantidade_check CHECK ((quantidade > (0)::numeric)),
    CONSTRAINT orcamento_itens_tempo_estimado_horas_check CHECK (((tempo_estimado_horas IS NULL) OR (tempo_estimado_horas >= (0)::numeric))),
    CONSTRAINT orcamento_itens_valor_unitario_check CHECK ((valor_unitario >= (0)::numeric))
);


--
-- Name: orcamento_itens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.orcamento_itens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.orcamento_itens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: orcamentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamentos (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    numero integer NOT NULL,
    cliente_id bigint NOT NULL,
    veiculo_id bigint NOT NULL,
    criado_por bigint,
    ordem_servico_id bigint,
    status character varying(20) DEFAULT 'RASCUNHO'::public.status_orcamento NOT NULL,
    validade date,
    observacoes text,
    desconto numeric(12,2) DEFAULT 0 NOT NULL,
    emitido_em timestamp with time zone,
    aprovado_em timestamp with time zone,
    rejeitado_em timestamp with time zone,
    motivo_rejeicao text,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    titulo character varying,
    tipo character varying,
    vendedor_id bigint,
    canal_envio character varying,
    enviado_em timestamp with time zone,
    visualizado_em timestamp with time zone,
    decidido_em timestamp with time zone,
    canal_decisao character varying,
    desconto_tipo character varying DEFAULT 'VALOR'::character varying NOT NULL,
    validade_dias integer DEFAULT 7 NOT NULL,
    convertido_em timestamp with time zone,
    atualizado_por bigint,
    CONSTRAINT orcamentos_desconto_check CHECK ((desconto >= (0)::numeric)),
    CONSTRAINT orcamentos_desconto_tipo_check CHECK (((desconto_tipo)::text = ANY ((ARRAY['VALOR'::character varying, 'PERCENTUAL'::character varying])::text[]))),
    CONSTRAINT orcamentos_validade_dias_check CHECK ((validade_dias >= 1))
);


--
-- Name: orcamentos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.orcamentos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.orcamentos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ordens_servico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ordens_servico (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    numero integer NOT NULL,
    cliente_id bigint NOT NULL,
    veiculo_id bigint NOT NULL,
    box_id bigint,
    usuario_abertura_id bigint,
    mecanico_lider_id bigint,
    status_id smallint NOT NULL,
    prioridade public.prioridade_os DEFAULT 'NORMAL'::public.prioridade_os NOT NULL,
    km_entrada integer,
    diagnostico text,
    observacoes text,
    desconto numeric(12,2) DEFAULT 0 NOT NULL,
    garantia_dias integer,
    garantia_km integer,
    aberta_em timestamp with time zone DEFAULT now() NOT NULL,
    fechada_em timestamp with time zone,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    tipo character varying,
    canal character varying,
    titulo character varying,
    relato_cliente text,
    observacoes_internas text,
    observacoes_cliente text,
    previsao_entrega timestamp with time zone,
    nivel_combustivel smallint,
    desconto_tipo character varying DEFAULT 'VALOR'::character varying NOT NULL,
    km_saida integer,
    diagnostico_validado boolean DEFAULT false NOT NULL,
    pronta_em timestamp with time zone,
    entregue_em timestamp with time zone,
    cancelada_em timestamp with time zone,
    motivo_cancelamento text,
    forma_pagamento character varying,
    parcelas smallint DEFAULT 1 NOT NULL,
    aprovacao_status character varying,
    aprovacao_canal character varying,
    aprovacao_enviada_em timestamp with time zone,
    aprovacao_decidida_em timestamp with time zone,
    aprovacao_visualizada_em timestamp with time zone,
    garantia_tipo character varying,
    atualizado_por bigint,
    CONSTRAINT ordens_servico_aprovacao_status_check CHECK (((aprovacao_status IS NULL) OR ((aprovacao_status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'APROVADO'::character varying, 'RECUSADO'::character varying])::text[])))),
    CONSTRAINT ordens_servico_desconto_check CHECK ((desconto >= (0)::numeric)),
    CONSTRAINT ordens_servico_desconto_tipo_check CHECK (((desconto_tipo)::text = ANY ((ARRAY['VALOR'::character varying, 'PERCENTUAL'::character varying])::text[]))),
    CONSTRAINT ordens_servico_garantia_dias_check CHECK (((garantia_dias IS NULL) OR (garantia_dias >= 0))),
    CONSTRAINT ordens_servico_garantia_km_check CHECK (((garantia_km IS NULL) OR (garantia_km >= 0))),
    CONSTRAINT ordens_servico_km_entrada_check CHECK (((km_entrada IS NULL) OR (km_entrada >= 0))),
    CONSTRAINT ordens_servico_km_saida_check CHECK (((km_saida IS NULL) OR (km_saida >= 0))),
    CONSTRAINT ordens_servico_nivel_combustivel_check CHECK (((nivel_combustivel IS NULL) OR ((nivel_combustivel >= 0) AND (nivel_combustivel <= 4)))),
    CONSTRAINT ordens_servico_parcelas_check CHECK ((parcelas >= 1))
);


--
-- Name: ordens_servico_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.ordens_servico ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.ordens_servico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: os_checklist; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.os_checklist (
    id bigint NOT NULL,
    ordem_servico_id bigint NOT NULL,
    checklist_item_id bigint NOT NULL,
    status public.status_checklist DEFAULT 'NAO_VERIFICADO'::public.status_checklist NOT NULL,
    observacao text,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: os_checklist_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.os_checklist ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.os_checklist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: os_historico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.os_historico (
    id bigint NOT NULL,
    ordem_servico_id bigint NOT NULL,
    usuario_id bigint,
    status_anterior_id smallint,
    status_novo_id smallint,
    descricao text NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    tipo_evento character varying DEFAULT 'STATUS'::character varying NOT NULL,
    metadata jsonb
);


--
-- Name: os_historico_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.os_historico ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.os_historico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: os_mecanicos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.os_mecanicos (
    id bigint NOT NULL,
    ordem_servico_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    funcao character varying(50)
);


--
-- Name: os_mecanicos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.os_mecanicos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.os_mecanicos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: os_pecas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.os_pecas (
    id bigint NOT NULL,
    ordem_servico_id bigint NOT NULL,
    peca_id bigint,
    descricao character varying(255) NOT NULL,
    quantidade numeric(12,3) NOT NULL,
    valor_unitario numeric(12,2) NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    cotada boolean DEFAULT true NOT NULL,
    instalada boolean DEFAULT false NOT NULL,
    instalada_em timestamp with time zone,
    desconto numeric DEFAULT 0 NOT NULL,
    desconto_tipo character varying DEFAULT 'VALOR'::character varying NOT NULL,
    unidade character varying,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT os_pecas_desconto_check CHECK ((desconto >= (0)::numeric)),
    CONSTRAINT os_pecas_desconto_tipo_check CHECK (((desconto_tipo)::text = ANY ((ARRAY['VALOR'::character varying, 'PERCENTUAL'::character varying])::text[]))),
    CONSTRAINT os_pecas_quantidade_check CHECK ((quantidade > (0)::numeric)),
    CONSTRAINT os_pecas_valor_unitario_check CHECK ((valor_unitario >= (0)::numeric))
);


--
-- Name: os_pecas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.os_pecas ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.os_pecas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: os_servicos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.os_servicos (
    id bigint NOT NULL,
    ordem_servico_id bigint NOT NULL,
    servico_id bigint,
    mecanico_id bigint,
    descricao character varying(255) NOT NULL,
    quantidade numeric(12,3) DEFAULT 1 NOT NULL,
    valor_unitario numeric(12,2) NOT NULL,
    tempo_estimado_horas numeric(8,2),
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    concluido boolean DEFAULT false NOT NULL,
    concluido_em timestamp with time zone,
    desconto numeric DEFAULT 0 NOT NULL,
    desconto_tipo character varying DEFAULT 'VALOR'::character varying NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT os_servicos_desconto_check CHECK ((desconto >= (0)::numeric)),
    CONSTRAINT os_servicos_desconto_tipo_check CHECK (((desconto_tipo)::text = ANY ((ARRAY['VALOR'::character varying, 'PERCENTUAL'::character varying])::text[]))),
    CONSTRAINT os_servicos_quantidade_check CHECK ((quantidade > (0)::numeric)),
    CONSTRAINT os_servicos_valor_unitario_check CHECK ((valor_unitario >= (0)::numeric))
);


--
-- Name: os_servicos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.os_servicos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.os_servicos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: os_vistoria_danos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.os_vistoria_danos (
    id bigint NOT NULL,
    vistoria_id bigint NOT NULL,
    area character varying NOT NULL,
    tipo character varying NOT NULL,
    observacao text,
    criado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: os_vistoria_danos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.os_vistoria_danos ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.os_vistoria_danos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: os_vistoria_fotos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.os_vistoria_fotos (
    id bigint NOT NULL,
    vistoria_id bigint NOT NULL,
    url text NOT NULL,
    storage_path text,
    legenda character varying,
    criado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: os_vistoria_fotos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.os_vistoria_fotos ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.os_vistoria_fotos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: os_vistorias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.os_vistorias (
    id bigint NOT NULL,
    ordem_servico_id bigint NOT NULL,
    cliente_ciente boolean DEFAULT false NOT NULL,
    pertences text,
    observacoes text,
    realizada_em timestamp with time zone,
    realizada_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE os_vistorias; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.os_vistorias IS 'Vistoria de entrada da OS: dados gerais, danos e fotos.';


--
-- Name: os_vistorias_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.os_vistorias ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.os_vistorias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pagamentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pagamentos (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    ordem_servico_id bigint,
    orcamento_id bigint,
    cliente_id bigint,
    descricao character varying,
    valor numeric(12,2) NOT NULL,
    forma_pagamento character varying,
    parcelas smallint DEFAULT 1 NOT NULL,
    status character varying DEFAULT 'PENDENTE'::character varying NOT NULL,
    vencimento date,
    pago_em timestamp with time zone,
    observacoes text,
    criado_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pagamentos_parcelas_check CHECK ((parcelas >= 1)),
    CONSTRAINT pagamentos_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'PAGO'::character varying, 'CANCELADO'::character varying, 'ESTORNADO'::character varying])::text[]))),
    CONSTRAINT pagamentos_valor_check CHECK ((valor > (0)::numeric))
);


--
-- Name: TABLE pagamentos; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.pagamentos IS 'Contas a receber (OS, orçamento ou avulso).';


--
-- Name: pagamentos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pagamentos ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pagamentos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pecas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pecas (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    codigo character varying(50) NOT NULL,
    nome character varying(150) NOT NULL,
    descricao text,
    fabricante character varying(100),
    preco_custo numeric(12,2) DEFAULT 0 NOT NULL,
    preco_venda numeric(12,2) DEFAULT 0 NOT NULL,
    estoque_atual numeric(12,3) DEFAULT 0 NOT NULL,
    estoque_minimo numeric(12,3) DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    oem character varying,
    categoria character varying,
    aplicacao text,
    unidade character varying DEFAULT 'un'::character varying NOT NULL,
    estoque_maximo numeric DEFAULT 0 NOT NULL,
    localizacao character varying,
    fornecedor_id bigint,
    observacao text,
    estoque_reservado numeric DEFAULT 0 NOT NULL,
    codigo_barras character varying,
    garantia_dias integer,
    ncm character varying,
    estoque_disponivel numeric GENERATED ALWAYS AS ((estoque_atual - estoque_reservado)) STORED,
    CONSTRAINT pecas_check CHECK (((estoque_reservado >= (0)::numeric) AND (estoque_reservado <= estoque_atual))),
    CONSTRAINT pecas_estoque_atual_check CHECK ((estoque_atual >= (0)::numeric)),
    CONSTRAINT pecas_estoque_maximo_check CHECK ((estoque_maximo >= (0)::numeric)),
    CONSTRAINT pecas_estoque_minimo_check CHECK ((estoque_minimo >= (0)::numeric)),
    CONSTRAINT pecas_garantia_dias_check CHECK (((garantia_dias IS NULL) OR (garantia_dias >= 0))),
    CONSTRAINT pecas_preco_custo_check CHECK ((preco_custo >= (0)::numeric)),
    CONSTRAINT pecas_preco_venda_check CHECK ((preco_venda >= (0)::numeric))
);


--
-- Name: COLUMN pecas.estoque_disponivel; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pecas.estoque_disponivel IS 'Saldo físico menos reservado (coluna gerada).';


--
-- Name: pecas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pecas ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.pecas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pedido_compra_itens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pedido_compra_itens (
    id bigint NOT NULL,
    pedido_compra_id bigint NOT NULL,
    peca_id bigint,
    descricao character varying NOT NULL,
    quantidade numeric NOT NULL,
    custo_unitario numeric DEFAULT 0 NOT NULL,
    quantidade_recebida numeric DEFAULT 0 NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pedido_compra_itens_custo_unitario_check CHECK ((custo_unitario >= (0)::numeric)),
    CONSTRAINT pedido_compra_itens_quantidade_check CHECK ((quantidade > (0)::numeric)),
    CONSTRAINT pedido_compra_itens_quantidade_recebida_check CHECK ((quantidade_recebida >= (0)::numeric)),
    CONSTRAINT pedido_compra_itens_recebido_chk CHECK ((quantidade_recebida <= quantidade))
);


--
-- Name: pedido_compra_itens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pedido_compra_itens ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pedido_compra_itens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pedidos_compra; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pedidos_compra (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    numero integer NOT NULL,
    fornecedor_id bigint NOT NULL,
    status character varying DEFAULT 'RASCUNHO'::character varying NOT NULL,
    data_pedido date DEFAULT CURRENT_DATE NOT NULL,
    previsao_entrega date,
    recebido_em timestamp with time zone,
    nota_fiscal character varying,
    observacoes text,
    criado_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pedidos_compra_status_check CHECK (((status)::text = ANY ((ARRAY['RASCUNHO'::character varying, 'ENVIADO'::character varying, 'RECEBIDO'::character varying, 'CANCELADO'::character varying])::text[])))
);


--
-- Name: TABLE pedidos_compra; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.pedidos_compra IS 'Pedido de compra de peças; numero sequencial por oficina.';


--
-- Name: pedidos_compra_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.pedidos_compra ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pedidos_compra_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: servico_pecas_sugeridas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.servico_pecas_sugeridas (
    servico_id bigint NOT NULL,
    peca_id bigint NOT NULL
);


--
-- Name: servicos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.servicos (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    codigo character varying(30) NOT NULL,
    nome character varying(150) NOT NULL,
    descricao text,
    preco_base numeric(12,2) DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    categoria character varying,
    detalhe text,
    tempo_estimado_horas numeric,
    garantia_tipo character varying,
    CONSTRAINT servicos_preco_base_check CHECK ((preco_base >= (0)::numeric)),
    CONSTRAINT servicos_tempo_estimado_horas_check CHECK (((tempo_estimado_horas IS NULL) OR (tempo_estimado_horas >= (0)::numeric)))
);


--
-- Name: servicos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.servicos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.servicos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: status_ordens_servico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.status_ordens_servico (
    id smallint NOT NULL,
    nome character varying(50) NOT NULL,
    ordem smallint DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL
);


--
-- Name: status_ordens_servico_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.status_ordens_servico ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.status_ordens_servico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuario_permissoes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario_permissoes (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    modulo character varying(30) NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT usuario_permissoes_modulo_chk CHECK (((modulo)::text = ANY ((ARRAY['CLIENTES'::character varying, 'VEICULOS'::character varying, 'OS'::character varying, 'ORCAMENTOS'::character varying, 'ESTOQUE'::character varying, 'AGENDA'::character varying, 'FINANCEIRO'::character varying])::text[])))
);


--
-- Name: usuario_permissoes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuario_permissoes ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.usuario_permissoes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id bigint NOT NULL,
    oficina_id bigint NOT NULL,
    auth_user_id uuid,
    nome character varying(150) NOT NULL,
    email character varying(150) NOT NULL,
    cargo character varying(50) NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    telefone character varying,
    especialidades character varying,
    lider boolean DEFAULT false NOT NULL,
    comissao_percentual numeric(5,2) DEFAULT 0 NOT NULL,
    cor_agenda character varying(7),
    foto_url text,
    perfil character varying(20) DEFAULT 'FUNCIONARIO'::character varying NOT NULL,
    executa_os boolean DEFAULT true NOT NULL,
    deve_trocar_senha boolean DEFAULT false NOT NULL,
    dono boolean DEFAULT false NOT NULL,
    CONSTRAINT usuarios_comissao_percentual_check CHECK (((comissao_percentual >= (0)::numeric) AND (comissao_percentual <= (100)::numeric))),
    CONSTRAINT usuarios_perfil_chk CHECK (((perfil)::text = ANY ((ARRAY['ADMIN'::character varying, 'FUNCIONARIO'::character varying])::text[])))
);


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: veiculos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.veiculos (
    id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    placa character varying(8) NOT NULL,
    chassi character varying(30),
    marca character varying(80) NOT NULL,
    modelo character varying(100) NOT NULL,
    versao character varying(100),
    ano_fabricacao smallint,
    ano_modelo smallint,
    cor character varying(50),
    combustivel character varying(30),
    motorizacao character varying(50),
    transmissao character varying(50),
    km_atual integer DEFAULT 0 NOT NULL,
    foto_url character varying(500),
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    oficina_id bigint NOT NULL,
    categoria character varying,
    rascunho boolean DEFAULT false NOT NULL,
    CONSTRAINT veiculos_km_atual_check CHECK ((km_atual >= 0))
);


--
-- Name: COLUMN veiculos.oficina_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.veiculos.oficina_id IS 'Oficina dona do veículo; preenchida a partir do cliente.';


--
-- Name: veiculos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.veiculos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.veiculos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vw_ordem_servico_resumo; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_ordem_servico_resumo AS
 SELECT os.id,
    os.oficina_id,
    os.numero,
    os.cliente_id,
    os.veiculo_id,
    os.status_id,
    os.prioridade,
    (COALESCE(s.total_servicos, (0)::numeric))::numeric(12,2) AS total_servicos,
    (COALESCE(p.total_pecas, (0)::numeric))::numeric(12,2) AS total_pecas,
    (((COALESCE(s.total_servicos, (0)::numeric) + COALESCE(p.total_pecas, (0)::numeric)) - os.desconto))::numeric(12,2) AS total
   FROM ((public.ordens_servico os
     LEFT JOIN ( SELECT os_servicos.ordem_servico_id,
            sum((os_servicos.quantidade * os_servicos.valor_unitario)) AS total_servicos
           FROM public.os_servicos
          GROUP BY os_servicos.ordem_servico_id) s ON ((s.ordem_servico_id = os.id)))
     LEFT JOIN ( SELECT os_pecas.ordem_servico_id,
            sum((os_pecas.quantidade * os_pecas.valor_unitario)) AS total_pecas
           FROM public.os_pecas
          GROUP BY os_pecas.ordem_servico_id) p ON ((p.ordem_servico_id = os.id)));


--
-- Name: agendamentos agendamentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_pkey PRIMARY KEY (id);


--
-- Name: auditoria auditoria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_pkey PRIMARY KEY (id);


--
-- Name: boxes boxes_oficina_id_nome_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boxes
    ADD CONSTRAINT boxes_oficina_id_nome_key UNIQUE (oficina_id, nome);


--
-- Name: boxes boxes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boxes
    ADD CONSTRAINT boxes_pkey PRIMARY KEY (id);


--
-- Name: checklist_itens checklist_itens_oficina_id_nome_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.checklist_itens
    ADD CONSTRAINT checklist_itens_oficina_id_nome_key UNIQUE (oficina_id, nome);


--
-- Name: checklist_itens checklist_itens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.checklist_itens
    ADD CONSTRAINT checklist_itens_pkey PRIMARY KEY (id);


--
-- Name: clientes clientes_oficina_id_cpf_cnpj_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clientes
    ADD CONSTRAINT clientes_oficina_id_cpf_cnpj_key UNIQUE (oficina_id, cpf_cnpj);


--
-- Name: clientes clientes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clientes
    ADD CONSTRAINT clientes_pkey PRIMARY KEY (id);


--
-- Name: despesas despesas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.despesas
    ADD CONSTRAINT despesas_pkey PRIMARY KEY (id);


--
-- Name: enderecos_clientes enderecos_clientes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enderecos_clientes
    ADD CONSTRAINT enderecos_clientes_pkey PRIMARY KEY (id);


--
-- Name: estoque_movimentacoes estoque_movimentacoes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimentacoes
    ADD CONSTRAINT estoque_movimentacoes_pkey PRIMARY KEY (id);


--
-- Name: fornecedores fornecedores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fornecedores
    ADD CONSTRAINT fornecedores_pkey PRIMARY KEY (id);


--
-- Name: notificacoes notificacoes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacoes
    ADD CONSTRAINT notificacoes_pkey PRIMARY KEY (id);


--
-- Name: oficina_sequencias oficina_sequencias_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oficina_sequencias
    ADD CONSTRAINT oficina_sequencias_pk PRIMARY KEY (oficina_id, entidade);


--
-- Name: oficinas oficinas_cnpj_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oficinas
    ADD CONSTRAINT oficinas_cnpj_key UNIQUE (cnpj);


--
-- Name: oficinas oficinas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oficinas
    ADD CONSTRAINT oficinas_pkey PRIMARY KEY (id);


--
-- Name: orcamento_aprovacoes orcamento_aprovacoes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_aprovacoes
    ADD CONSTRAINT orcamento_aprovacoes_pkey PRIMARY KEY (id);


--
-- Name: orcamento_aprovacoes orcamento_aprovacoes_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_aprovacoes
    ADD CONSTRAINT orcamento_aprovacoes_token_key UNIQUE (token);


--
-- Name: orcamento_itens orcamento_itens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_itens
    ADD CONSTRAINT orcamento_itens_pkey PRIMARY KEY (id);


--
-- Name: orcamentos orcamentos_oficina_id_numero_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_oficina_id_numero_key UNIQUE (oficina_id, numero);


--
-- Name: orcamentos orcamentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_pkey PRIMARY KEY (id);


--
-- Name: ordens_servico ordens_servico_oficina_id_numero_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_oficina_id_numero_key UNIQUE (oficina_id, numero);


--
-- Name: ordens_servico ordens_servico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_pkey PRIMARY KEY (id);


--
-- Name: os_checklist os_checklist_ordem_servico_id_checklist_item_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_checklist
    ADD CONSTRAINT os_checklist_ordem_servico_id_checklist_item_id_key UNIQUE (ordem_servico_id, checklist_item_id);


--
-- Name: os_checklist os_checklist_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_checklist
    ADD CONSTRAINT os_checklist_pkey PRIMARY KEY (id);


--
-- Name: os_historico os_historico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_historico
    ADD CONSTRAINT os_historico_pkey PRIMARY KEY (id);


--
-- Name: os_mecanicos os_mecanicos_ordem_servico_id_usuario_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_mecanicos
    ADD CONSTRAINT os_mecanicos_ordem_servico_id_usuario_id_key UNIQUE (ordem_servico_id, usuario_id);


--
-- Name: os_mecanicos os_mecanicos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_mecanicos
    ADD CONSTRAINT os_mecanicos_pkey PRIMARY KEY (id);


--
-- Name: os_pecas os_pecas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_pecas
    ADD CONSTRAINT os_pecas_pkey PRIMARY KEY (id);


--
-- Name: os_servicos os_servicos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_servicos
    ADD CONSTRAINT os_servicos_pkey PRIMARY KEY (id);


--
-- Name: os_vistoria_danos os_vistoria_danos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_vistoria_danos
    ADD CONSTRAINT os_vistoria_danos_pkey PRIMARY KEY (id);


--
-- Name: os_vistoria_fotos os_vistoria_fotos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_vistoria_fotos
    ADD CONSTRAINT os_vistoria_fotos_pkey PRIMARY KEY (id);


--
-- Name: os_vistorias os_vistorias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_vistorias
    ADD CONSTRAINT os_vistorias_pkey PRIMARY KEY (id);


--
-- Name: pagamentos pagamentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagamentos
    ADD CONSTRAINT pagamentos_pkey PRIMARY KEY (id);


--
-- Name: pecas pecas_oficina_id_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pecas
    ADD CONSTRAINT pecas_oficina_id_codigo_key UNIQUE (oficina_id, codigo);


--
-- Name: pecas pecas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pecas
    ADD CONSTRAINT pecas_pkey PRIMARY KEY (id);


--
-- Name: pedido_compra_itens pedido_compra_itens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pedido_compra_itens
    ADD CONSTRAINT pedido_compra_itens_pkey PRIMARY KEY (id);


--
-- Name: pedidos_compra pedidos_compra_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pedidos_compra
    ADD CONSTRAINT pedidos_compra_pkey PRIMARY KEY (id);


--
-- Name: servico_pecas_sugeridas servico_pecas_sugeridas_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.servico_pecas_sugeridas
    ADD CONSTRAINT servico_pecas_sugeridas_pk PRIMARY KEY (servico_id, peca_id);


--
-- Name: servicos servicos_oficina_id_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.servicos
    ADD CONSTRAINT servicos_oficina_id_codigo_key UNIQUE (oficina_id, codigo);


--
-- Name: servicos servicos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.servicos
    ADD CONSTRAINT servicos_pkey PRIMARY KEY (id);


--
-- Name: status_ordens_servico status_ordens_servico_nome_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.status_ordens_servico
    ADD CONSTRAINT status_ordens_servico_nome_key UNIQUE (nome);


--
-- Name: status_ordens_servico status_ordens_servico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.status_ordens_servico
    ADD CONSTRAINT status_ordens_servico_pkey PRIMARY KEY (id);


--
-- Name: usuario_permissoes usuario_permissoes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_permissoes
    ADD CONSTRAINT usuario_permissoes_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_auth_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_auth_user_id_key UNIQUE (auth_user_id);


--
-- Name: usuarios usuarios_oficina_id_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_oficina_id_email_key UNIQUE (oficina_id, email);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: usuario_permissoes ux_usuario_permissoes_usuario_modulo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_permissoes
    ADD CONSTRAINT ux_usuario_permissoes_usuario_modulo UNIQUE (usuario_id, modulo);


--
-- Name: veiculos veiculos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.veiculos
    ADD CONSTRAINT veiculos_pkey PRIMARY KEY (id);


--
-- Name: idx_boxes_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_boxes_oficina ON public.boxes USING btree (oficina_id);


--
-- Name: idx_clientes_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clientes_oficina ON public.clientes USING btree (oficina_id);


--
-- Name: idx_estoque_peca; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estoque_peca ON public.estoque_movimentacoes USING btree (peca_id);


--
-- Name: idx_orcamento_itens_orcamento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamento_itens_orcamento ON public.orcamento_itens USING btree (orcamento_id);


--
-- Name: idx_orcamentos_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamentos_cliente ON public.orcamentos USING btree (cliente_id);


--
-- Name: idx_orcamentos_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamentos_status ON public.orcamentos USING btree (status);


--
-- Name: idx_orcamentos_veiculo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamentos_veiculo ON public.orcamentos USING btree (veiculo_id);


--
-- Name: idx_os_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_cliente ON public.ordens_servico USING btree (cliente_id);


--
-- Name: idx_os_data; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_data ON public.ordens_servico USING btree (criado_em);


--
-- Name: idx_os_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_oficina ON public.ordens_servico USING btree (oficina_id);


--
-- Name: idx_os_pecas_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_pecas_os ON public.os_pecas USING btree (ordem_servico_id);


--
-- Name: idx_os_servicos_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_servicos_os ON public.os_servicos USING btree (ordem_servico_id);


--
-- Name: idx_os_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_status ON public.ordens_servico USING btree (status_id);


--
-- Name: idx_os_veiculo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_veiculo ON public.ordens_servico USING btree (veiculo_id);


--
-- Name: idx_usuarios_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_oficina ON public.usuarios USING btree (oficina_id);


--
-- Name: idx_veiculos_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_veiculos_cliente ON public.veiculos USING btree (cliente_id);


--
-- Name: ix_agendamentos_mecanico_inicio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_agendamentos_mecanico_inicio ON public.agendamentos USING btree (mecanico_id, inicio);


--
-- Name: ix_agendamentos_oficina_inicio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_agendamentos_oficina_inicio ON public.agendamentos USING btree (oficina_id, inicio);


--
-- Name: ix_agendamentos_veiculo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_agendamentos_veiculo ON public.agendamentos USING btree (veiculo_id);


--
-- Name: ix_auditoria_modulo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_auditoria_modulo ON public.auditoria USING btree (oficina_id, modulo, criado_em DESC);


--
-- Name: ix_auditoria_oficina_criado_em; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_auditoria_oficina_criado_em ON public.auditoria USING btree (oficina_id, criado_em DESC);


--
-- Name: ix_auditoria_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_auditoria_usuario ON public.auditoria USING btree (usuario_id, criado_em DESC);


--
-- Name: ix_boxes_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_boxes_oficina ON public.boxes USING btree (oficina_id);


--
-- Name: ix_checklist_itens_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_checklist_itens_oficina ON public.checklist_itens USING btree (oficina_id);


--
-- Name: ix_clientes_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_clientes_oficina ON public.clientes USING btree (oficina_id);


--
-- Name: ix_clientes_oficina_nome; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_clientes_oficina_nome ON public.clientes USING btree (oficina_id, nome);


--
-- Name: ix_despesas_fornecedor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_despesas_fornecedor ON public.despesas USING btree (fornecedor_id);


--
-- Name: ix_despesas_oficina_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_despesas_oficina_status ON public.despesas USING btree (oficina_id, status, vencimento);


--
-- Name: ix_enderecos_clientes_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_enderecos_clientes_cliente ON public.enderecos_clientes USING btree (cliente_id);


--
-- Name: ix_estoque_mov_fornecedor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_estoque_mov_fornecedor ON public.estoque_movimentacoes USING btree (fornecedor_id);


--
-- Name: ix_estoque_mov_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_estoque_mov_oficina ON public.estoque_movimentacoes USING btree (oficina_id, criado_em DESC);


--
-- Name: ix_estoque_mov_peca; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_estoque_mov_peca ON public.estoque_movimentacoes USING btree (peca_id);


--
-- Name: ix_estoque_mov_pedido; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_estoque_mov_pedido ON public.estoque_movimentacoes USING btree (pedido_compra_id);


--
-- Name: ix_fornecedores_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_fornecedores_oficina ON public.fornecedores USING btree (oficina_id, nome);


--
-- Name: ix_notificacoes_oficina_lida; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_notificacoes_oficina_lida ON public.notificacoes USING btree (oficina_id, lida, criado_em DESC);


--
-- Name: ix_notificacoes_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_notificacoes_usuario ON public.notificacoes USING btree (usuario_id, lida, criado_em DESC);


--
-- Name: ix_orcamento_aprovacoes_orcamento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_orcamento_aprovacoes_orcamento ON public.orcamento_aprovacoes USING btree (orcamento_id);


--
-- Name: ix_orcamento_itens_orcamento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_orcamento_itens_orcamento ON public.orcamento_itens USING btree (orcamento_id);


--
-- Name: ix_orcamento_itens_peca; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_orcamento_itens_peca ON public.orcamento_itens USING btree (peca_id);


--
-- Name: ix_orcamento_itens_servico; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_orcamento_itens_servico ON public.orcamento_itens USING btree (servico_id);


--
-- Name: ix_orcamentos_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_orcamentos_cliente ON public.orcamentos USING btree (cliente_id);


--
-- Name: ix_orcamentos_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_orcamentos_oficina ON public.orcamentos USING btree (oficina_id, numero DESC);


--
-- Name: ix_orcamentos_veiculo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_orcamentos_veiculo ON public.orcamentos USING btree (veiculo_id);


--
-- Name: ix_ordens_servico_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ordens_servico_cliente ON public.ordens_servico USING btree (cliente_id);


--
-- Name: ix_ordens_servico_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ordens_servico_oficina ON public.ordens_servico USING btree (oficina_id, numero DESC);


--
-- Name: ix_ordens_servico_oficina_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ordens_servico_oficina_status ON public.ordens_servico USING btree (oficina_id, status_id, numero DESC);


--
-- Name: ix_ordens_servico_previsao; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ordens_servico_previsao ON public.ordens_servico USING btree (oficina_id, previsao_entrega);


--
-- Name: ix_ordens_servico_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ordens_servico_status ON public.ordens_servico USING btree (status_id);


--
-- Name: ix_ordens_servico_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ordens_servico_tipo ON public.ordens_servico USING btree (oficina_id, tipo);


--
-- Name: ix_ordens_servico_veiculo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ordens_servico_veiculo ON public.ordens_servico USING btree (veiculo_id);


--
-- Name: ix_os_checklist_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_checklist_os ON public.os_checklist USING btree (ordem_servico_id);


--
-- Name: ix_os_historico_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_historico_os ON public.os_historico USING btree (ordem_servico_id, criado_em);


--
-- Name: ix_os_mecanicos_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_mecanicos_os ON public.os_mecanicos USING btree (ordem_servico_id);


--
-- Name: ix_os_pecas_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_pecas_os ON public.os_pecas USING btree (ordem_servico_id);


--
-- Name: ix_os_pecas_peca; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_pecas_peca ON public.os_pecas USING btree (peca_id);


--
-- Name: ix_os_servicos_mecanico; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_servicos_mecanico ON public.os_servicos USING btree (mecanico_id);


--
-- Name: ix_os_servicos_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_servicos_os ON public.os_servicos USING btree (ordem_servico_id);


--
-- Name: ix_os_servicos_servico; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_servicos_servico ON public.os_servicos USING btree (servico_id);


--
-- Name: ix_os_vistoria_danos_vistoria; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_vistoria_danos_vistoria ON public.os_vistoria_danos USING btree (vistoria_id);


--
-- Name: ix_os_vistoria_fotos_vistoria; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_os_vistoria_fotos_vistoria ON public.os_vistoria_fotos USING btree (vistoria_id);


--
-- Name: ix_pagamentos_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pagamentos_cliente ON public.pagamentos USING btree (cliente_id);


--
-- Name: ix_pagamentos_oficina_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pagamentos_oficina_status ON public.pagamentos USING btree (oficina_id, status, vencimento);


--
-- Name: ix_pagamentos_orcamento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pagamentos_orcamento ON public.pagamentos USING btree (orcamento_id);


--
-- Name: ix_pagamentos_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pagamentos_os ON public.pagamentos USING btree (ordem_servico_id);


--
-- Name: ix_pecas_fornecedor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pecas_fornecedor ON public.pecas USING btree (fornecedor_id);


--
-- Name: ix_pecas_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pecas_oficina ON public.pecas USING btree (oficina_id);


--
-- Name: ix_pecas_oficina_categoria; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pecas_oficina_categoria ON public.pecas USING btree (oficina_id, categoria);


--
-- Name: ix_pedido_compra_itens_peca; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pedido_compra_itens_peca ON public.pedido_compra_itens USING btree (peca_id);


--
-- Name: ix_pedido_compra_itens_pedido; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pedido_compra_itens_pedido ON public.pedido_compra_itens USING btree (pedido_compra_id);


--
-- Name: ix_pedidos_compra_fornecedor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pedidos_compra_fornecedor ON public.pedidos_compra USING btree (fornecedor_id);


--
-- Name: ix_pedidos_compra_oficina_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pedidos_compra_oficina_status ON public.pedidos_compra USING btree (oficina_id, status, data_pedido DESC);


--
-- Name: ix_servico_pecas_sugeridas_peca; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_servico_pecas_sugeridas_peca ON public.servico_pecas_sugeridas USING btree (peca_id);


--
-- Name: ix_servicos_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_servicos_oficina ON public.servicos USING btree (oficina_id);


--
-- Name: ix_usuario_permissoes_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_usuario_permissoes_usuario ON public.usuario_permissoes USING btree (usuario_id);


--
-- Name: ix_usuarios_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_usuarios_oficina ON public.usuarios USING btree (oficina_id);


--
-- Name: ix_usuarios_oficina_executa_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_usuarios_oficina_executa_os ON public.usuarios USING btree (oficina_id, executa_os, ativo);


--
-- Name: ix_usuarios_oficina_perfil_ativo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_usuarios_oficina_perfil_ativo ON public.usuarios USING btree (oficina_id, perfil, ativo);


--
-- Name: ix_veiculos_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_veiculos_cliente ON public.veiculos USING btree (cliente_id);


--
-- Name: ix_veiculos_oficina; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_veiculos_oficina ON public.veiculos USING btree (oficina_id);


--
-- Name: ix_veiculos_placa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_veiculos_placa ON public.veiculos USING btree (placa);


--
-- Name: ux_boxes_oficina_nome; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_boxes_oficina_nome ON public.boxes USING btree (oficina_id, lower(btrim((nome)::text)));


--
-- Name: ux_checklist_itens_oficina_codigo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_checklist_itens_oficina_codigo ON public.checklist_itens USING btree (oficina_id, codigo);


--
-- Name: ux_clientes_oficina_cpf_cnpj; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_clientes_oficina_cpf_cnpj ON public.clientes USING btree (oficina_id, cpf_cnpj);


--
-- Name: ux_enderecos_cliente_principal; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_enderecos_cliente_principal ON public.enderecos_clientes USING btree (cliente_id) WHERE principal;


--
-- Name: ux_fornecedores_oficina_cnpj; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_fornecedores_oficina_cnpj ON public.fornecedores USING btree (oficina_id, regexp_replace((cnpj)::text, '\D'::text, ''::text, 'g'::text)) WHERE (cnpj IS NOT NULL);


--
-- Name: ux_fornecedores_oficina_nome; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_fornecedores_oficina_nome ON public.fornecedores USING btree (oficina_id, lower(btrim((nome)::text)));


--
-- Name: ux_orcamento_aprovacoes_pendente; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_orcamento_aprovacoes_pendente ON public.orcamento_aprovacoes USING btree (orcamento_id) WHERE (status = 'PENDENTE'::public.status_aprovacao);


--
-- Name: ux_orcamentos_oficina_numero; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_orcamentos_oficina_numero ON public.orcamentos USING btree (oficina_id, numero);


--
-- Name: ux_ordens_servico_oficina_numero; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_ordens_servico_oficina_numero ON public.ordens_servico USING btree (oficina_id, numero);


--
-- Name: ux_os_checklist_os_item; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_os_checklist_os_item ON public.os_checklist USING btree (ordem_servico_id, checklist_item_id);


--
-- Name: ux_os_mecanicos_os_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_os_mecanicos_os_usuario ON public.os_mecanicos USING btree (ordem_servico_id, usuario_id);


--
-- Name: ux_os_vistoria_danos_area_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_os_vistoria_danos_area_tipo ON public.os_vistoria_danos USING btree (vistoria_id, area, tipo);


--
-- Name: ux_os_vistorias_os; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_os_vistorias_os ON public.os_vistorias USING btree (ordem_servico_id);


--
-- Name: ux_pecas_oficina_codigo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_pecas_oficina_codigo ON public.pecas USING btree (oficina_id, lower(btrim((codigo)::text)));


--
-- Name: ux_pecas_oficina_codigo_barras; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_pecas_oficina_codigo_barras ON public.pecas USING btree (oficina_id, codigo_barras) WHERE (codigo_barras IS NOT NULL);


--
-- Name: ux_pedidos_compra_oficina_numero; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_pedidos_compra_oficina_numero ON public.pedidos_compra USING btree (oficina_id, numero);


--
-- Name: ux_servicos_oficina_codigo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_servicos_oficina_codigo ON public.servicos USING btree (oficina_id, lower(btrim((codigo)::text)));


--
-- Name: ux_usuarios_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_usuarios_email ON public.usuarios USING btree (lower(btrim((email)::text)));


--
-- Name: ux_usuarios_oficina_dono; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_usuarios_oficina_dono ON public.usuarios USING btree (oficina_id) WHERE dono;


--
-- Name: ux_veiculos_oficina_chassi; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_veiculos_oficina_chassi ON public.veiculos USING btree (oficina_id, upper(btrim((chassi)::text))) WHERE (chassi IS NOT NULL);


--
-- Name: ux_veiculos_oficina_placa; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_veiculos_oficina_placa ON public.veiculos USING btree (oficina_id, upper(regexp_replace((placa)::text, '[^A-Za-z0-9]'::text, ''::text, 'g'::text)));


--
-- Name: agendamentos trg_agendamentos_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_agendamentos_updated_at BEFORE UPDATE ON public.agendamentos FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: clientes trg_clientes_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_clientes_updated_at BEFORE UPDATE ON public.clientes FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: despesas trg_despesas_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_despesas_updated_at BEFORE UPDATE ON public.despesas FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: enderecos_clientes trg_enderecos_clientes_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_enderecos_clientes_updated_at BEFORE UPDATE ON public.enderecos_clientes FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: estoque_movimentacoes trg_estoque_movimentacoes_aplica; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_estoque_movimentacoes_aplica BEFORE INSERT ON public.estoque_movimentacoes FOR EACH ROW EXECUTE FUNCTION public.fn_aplica_movimentacao_estoque();


--
-- Name: fornecedores trg_fornecedores_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_fornecedores_updated_at BEFORE UPDATE ON public.fornecedores FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: oficinas trg_oficinas_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_oficinas_updated_at BEFORE UPDATE ON public.oficinas FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: orcamento_aprovacoes trg_orcamento_aprovacoes_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_orcamento_aprovacoes_updated_at BEFORE UPDATE ON public.orcamento_aprovacoes FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: orcamento_itens trg_orcamento_itens_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_orcamento_itens_updated_at BEFORE UPDATE ON public.orcamento_itens FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: orcamentos trg_orcamentos_numero_imutavel; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_orcamentos_numero_imutavel BEFORE UPDATE ON public.orcamentos FOR EACH ROW EXECUTE FUNCTION public.fn_valida_numero_documento();


--
-- Name: orcamentos trg_orcamentos_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_orcamentos_updated_at BEFORE UPDATE ON public.orcamentos FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: ordens_servico trg_ordens_servico_numero_imutavel; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_ordens_servico_numero_imutavel BEFORE UPDATE ON public.ordens_servico FOR EACH ROW EXECUTE FUNCTION public.fn_valida_numero_ordem_servico();


--
-- Name: ordens_servico trg_ordens_servico_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_ordens_servico_updated_at BEFORE UPDATE ON public.ordens_servico FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: os_checklist trg_os_checklist_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_os_checklist_updated_at BEFORE UPDATE ON public.os_checklist FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: os_pecas trg_os_pecas_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_os_pecas_updated_at BEFORE UPDATE ON public.os_pecas FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: os_servicos trg_os_servicos_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_os_servicos_updated_at BEFORE UPDATE ON public.os_servicos FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: os_vistorias trg_os_vistorias_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_os_vistorias_updated_at BEFORE UPDATE ON public.os_vistorias FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: pagamentos trg_pagamentos_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_pagamentos_updated_at BEFORE UPDATE ON public.pagamentos FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: pecas trg_pecas_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_pecas_updated_at BEFORE UPDATE ON public.pecas FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: pedidos_compra trg_pedidos_compra_numero_imutavel; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_pedidos_compra_numero_imutavel BEFORE UPDATE ON public.pedidos_compra FOR EACH ROW EXECUTE FUNCTION public.fn_valida_numero_documento();


--
-- Name: pedidos_compra trg_pedidos_compra_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_pedidos_compra_updated_at BEFORE UPDATE ON public.pedidos_compra FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: servicos trg_servicos_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_servicos_updated_at BEFORE UPDATE ON public.servicos FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: usuarios trg_usuarios_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_usuarios_updated_at BEFORE UPDATE ON public.usuarios FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: veiculos trg_veiculos_oficina; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_veiculos_oficina BEFORE INSERT OR UPDATE OF cliente_id, oficina_id ON public.veiculos FOR EACH ROW EXECUTE FUNCTION public.fn_preenche_veiculo_oficina();


--
-- Name: veiculos trg_veiculos_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_veiculos_updated_at BEFORE UPDATE ON public.veiculos FOR EACH ROW EXECUTE FUNCTION public.fn_atualiza_atualizado_em();


--
-- Name: agendamentos agendamentos_box_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_box_id_fkey FOREIGN KEY (box_id) REFERENCES public.boxes(id) ON DELETE SET NULL;


--
-- Name: agendamentos agendamentos_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.clientes(id) ON DELETE SET NULL;


--
-- Name: agendamentos agendamentos_criado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_criado_por_fkey FOREIGN KEY (criado_por) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: agendamentos agendamentos_mecanico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_mecanico_id_fkey FOREIGN KEY (mecanico_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: agendamentos agendamentos_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: agendamentos agendamentos_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE SET NULL;


--
-- Name: agendamentos agendamentos_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_servico_id_fkey FOREIGN KEY (servico_id) REFERENCES public.servicos(id) ON DELETE SET NULL;


--
-- Name: agendamentos agendamentos_veiculo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agendamentos
    ADD CONSTRAINT agendamentos_veiculo_id_fkey FOREIGN KEY (veiculo_id) REFERENCES public.veiculos(id) ON DELETE SET NULL;


--
-- Name: auditoria auditoria_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id);


--
-- Name: auditoria auditoria_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: boxes boxes_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.boxes
    ADD CONSTRAINT boxes_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: checklist_itens checklist_itens_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.checklist_itens
    ADD CONSTRAINT checklist_itens_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: clientes clientes_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clientes
    ADD CONSTRAINT clientes_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: despesas despesas_criado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.despesas
    ADD CONSTRAINT despesas_criado_por_fkey FOREIGN KEY (criado_por) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: despesas despesas_fornecedor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.despesas
    ADD CONSTRAINT despesas_fornecedor_id_fkey FOREIGN KEY (fornecedor_id) REFERENCES public.fornecedores(id) ON DELETE SET NULL;


--
-- Name: despesas despesas_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.despesas
    ADD CONSTRAINT despesas_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: enderecos_clientes enderecos_clientes_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enderecos_clientes
    ADD CONSTRAINT enderecos_clientes_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.clientes(id) ON DELETE CASCADE;


--
-- Name: estoque_movimentacoes estoque_movimentacoes_fornecedor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimentacoes
    ADD CONSTRAINT estoque_movimentacoes_fornecedor_id_fkey FOREIGN KEY (fornecedor_id) REFERENCES public.fornecedores(id) ON DELETE SET NULL;


--
-- Name: estoque_movimentacoes estoque_movimentacoes_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimentacoes
    ADD CONSTRAINT estoque_movimentacoes_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: estoque_movimentacoes estoque_movimentacoes_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimentacoes
    ADD CONSTRAINT estoque_movimentacoes_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE SET NULL;


--
-- Name: estoque_movimentacoes estoque_movimentacoes_peca_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimentacoes
    ADD CONSTRAINT estoque_movimentacoes_peca_id_fkey FOREIGN KEY (peca_id) REFERENCES public.pecas(id) ON DELETE RESTRICT;


--
-- Name: estoque_movimentacoes estoque_movimentacoes_pedido_compra_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimentacoes
    ADD CONSTRAINT estoque_movimentacoes_pedido_compra_id_fkey FOREIGN KEY (pedido_compra_id) REFERENCES public.pedidos_compra(id) ON DELETE SET NULL;


--
-- Name: estoque_movimentacoes estoque_movimentacoes_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimentacoes
    ADD CONSTRAINT estoque_movimentacoes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: veiculos fk_veiculos_oficina; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.veiculos
    ADD CONSTRAINT fk_veiculos_oficina FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id);


--
-- Name: fornecedores fornecedores_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fornecedores
    ADD CONSTRAINT fornecedores_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: notificacoes notificacoes_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacoes
    ADD CONSTRAINT notificacoes_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: notificacoes notificacoes_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacoes
    ADD CONSTRAINT notificacoes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;


--
-- Name: oficina_sequencias oficina_sequencias_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oficina_sequencias
    ADD CONSTRAINT oficina_sequencias_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: orcamento_aprovacoes orcamento_aprovacoes_orcamento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_aprovacoes
    ADD CONSTRAINT orcamento_aprovacoes_orcamento_id_fkey FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id) ON DELETE CASCADE;


--
-- Name: orcamento_aprovacoes orcamento_aprovacoes_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_aprovacoes
    ADD CONSTRAINT orcamento_aprovacoes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: orcamento_itens orcamento_itens_mecanico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_itens
    ADD CONSTRAINT orcamento_itens_mecanico_id_fkey FOREIGN KEY (mecanico_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: orcamento_itens orcamento_itens_orcamento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_itens
    ADD CONSTRAINT orcamento_itens_orcamento_id_fkey FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id) ON DELETE CASCADE;


--
-- Name: orcamento_itens orcamento_itens_peca_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_itens
    ADD CONSTRAINT orcamento_itens_peca_id_fkey FOREIGN KEY (peca_id) REFERENCES public.pecas(id) ON DELETE SET NULL;


--
-- Name: orcamento_itens orcamento_itens_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_itens
    ADD CONSTRAINT orcamento_itens_servico_id_fkey FOREIGN KEY (servico_id) REFERENCES public.servicos(id) ON DELETE SET NULL;


--
-- Name: orcamentos orcamentos_atualizado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_atualizado_por_fkey FOREIGN KEY (atualizado_por) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: orcamentos orcamentos_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.clientes(id) ON DELETE RESTRICT;


--
-- Name: orcamentos orcamentos_criado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_criado_por_fkey FOREIGN KEY (criado_por) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: orcamentos orcamentos_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: orcamentos orcamentos_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE SET NULL;


--
-- Name: orcamentos orcamentos_veiculo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_veiculo_id_fkey FOREIGN KEY (veiculo_id) REFERENCES public.veiculos(id) ON DELETE RESTRICT;


--
-- Name: orcamentos orcamentos_vendedor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_vendedor_id_fkey FOREIGN KEY (vendedor_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: ordens_servico ordens_servico_atualizado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_atualizado_por_fkey FOREIGN KEY (atualizado_por) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: ordens_servico ordens_servico_box_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_box_id_fkey FOREIGN KEY (box_id) REFERENCES public.boxes(id) ON DELETE SET NULL;


--
-- Name: ordens_servico ordens_servico_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.clientes(id) ON DELETE RESTRICT;


--
-- Name: ordens_servico ordens_servico_mecanico_lider_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_mecanico_lider_id_fkey FOREIGN KEY (mecanico_lider_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: ordens_servico ordens_servico_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: ordens_servico ordens_servico_status_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_status_id_fkey FOREIGN KEY (status_id) REFERENCES public.status_ordens_servico(id);


--
-- Name: ordens_servico ordens_servico_usuario_abertura_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_usuario_abertura_id_fkey FOREIGN KEY (usuario_abertura_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: ordens_servico ordens_servico_veiculo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_veiculo_id_fkey FOREIGN KEY (veiculo_id) REFERENCES public.veiculos(id) ON DELETE RESTRICT;


--
-- Name: os_checklist os_checklist_checklist_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_checklist
    ADD CONSTRAINT os_checklist_checklist_item_id_fkey FOREIGN KEY (checklist_item_id) REFERENCES public.checklist_itens(id) ON DELETE RESTRICT;


--
-- Name: os_checklist os_checklist_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_checklist
    ADD CONSTRAINT os_checklist_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE CASCADE;


--
-- Name: os_historico os_historico_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_historico
    ADD CONSTRAINT os_historico_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE CASCADE;


--
-- Name: os_historico os_historico_status_anterior_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_historico
    ADD CONSTRAINT os_historico_status_anterior_id_fkey FOREIGN KEY (status_anterior_id) REFERENCES public.status_ordens_servico(id);


--
-- Name: os_historico os_historico_status_novo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_historico
    ADD CONSTRAINT os_historico_status_novo_id_fkey FOREIGN KEY (status_novo_id) REFERENCES public.status_ordens_servico(id);


--
-- Name: os_historico os_historico_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_historico
    ADD CONSTRAINT os_historico_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: os_mecanicos os_mecanicos_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_mecanicos
    ADD CONSTRAINT os_mecanicos_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE CASCADE;


--
-- Name: os_mecanicos os_mecanicos_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_mecanicos
    ADD CONSTRAINT os_mecanicos_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE RESTRICT;


--
-- Name: os_pecas os_pecas_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_pecas
    ADD CONSTRAINT os_pecas_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE CASCADE;


--
-- Name: os_pecas os_pecas_peca_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_pecas
    ADD CONSTRAINT os_pecas_peca_id_fkey FOREIGN KEY (peca_id) REFERENCES public.pecas(id) ON DELETE SET NULL;


--
-- Name: os_servicos os_servicos_mecanico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_servicos
    ADD CONSTRAINT os_servicos_mecanico_id_fkey FOREIGN KEY (mecanico_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: os_servicos os_servicos_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_servicos
    ADD CONSTRAINT os_servicos_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE CASCADE;


--
-- Name: os_servicos os_servicos_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_servicos
    ADD CONSTRAINT os_servicos_servico_id_fkey FOREIGN KEY (servico_id) REFERENCES public.servicos(id) ON DELETE SET NULL;


--
-- Name: os_vistoria_danos os_vistoria_danos_vistoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_vistoria_danos
    ADD CONSTRAINT os_vistoria_danos_vistoria_id_fkey FOREIGN KEY (vistoria_id) REFERENCES public.os_vistorias(id) ON DELETE CASCADE;


--
-- Name: os_vistoria_fotos os_vistoria_fotos_vistoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_vistoria_fotos
    ADD CONSTRAINT os_vistoria_fotos_vistoria_id_fkey FOREIGN KEY (vistoria_id) REFERENCES public.os_vistorias(id) ON DELETE CASCADE;


--
-- Name: os_vistorias os_vistorias_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_vistorias
    ADD CONSTRAINT os_vistorias_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE CASCADE;


--
-- Name: os_vistorias os_vistorias_realizada_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.os_vistorias
    ADD CONSTRAINT os_vistorias_realizada_por_fkey FOREIGN KEY (realizada_por) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: pagamentos pagamentos_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagamentos
    ADD CONSTRAINT pagamentos_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.clientes(id) ON DELETE SET NULL;


--
-- Name: pagamentos pagamentos_criado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagamentos
    ADD CONSTRAINT pagamentos_criado_por_fkey FOREIGN KEY (criado_por) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: pagamentos pagamentos_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagamentos
    ADD CONSTRAINT pagamentos_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: pagamentos pagamentos_orcamento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagamentos
    ADD CONSTRAINT pagamentos_orcamento_id_fkey FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id) ON DELETE SET NULL;


--
-- Name: pagamentos pagamentos_ordem_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagamentos
    ADD CONSTRAINT pagamentos_ordem_servico_id_fkey FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id) ON DELETE SET NULL;


--
-- Name: pecas pecas_fornecedor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pecas
    ADD CONSTRAINT pecas_fornecedor_id_fkey FOREIGN KEY (fornecedor_id) REFERENCES public.fornecedores(id) ON DELETE SET NULL;


--
-- Name: pecas pecas_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pecas
    ADD CONSTRAINT pecas_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: pedido_compra_itens pedido_compra_itens_peca_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pedido_compra_itens
    ADD CONSTRAINT pedido_compra_itens_peca_id_fkey FOREIGN KEY (peca_id) REFERENCES public.pecas(id) ON DELETE SET NULL;


--
-- Name: pedido_compra_itens pedido_compra_itens_pedido_compra_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pedido_compra_itens
    ADD CONSTRAINT pedido_compra_itens_pedido_compra_id_fkey FOREIGN KEY (pedido_compra_id) REFERENCES public.pedidos_compra(id) ON DELETE CASCADE;


--
-- Name: pedidos_compra pedidos_compra_criado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pedidos_compra
    ADD CONSTRAINT pedidos_compra_criado_por_fkey FOREIGN KEY (criado_por) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: pedidos_compra pedidos_compra_fornecedor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pedidos_compra
    ADD CONSTRAINT pedidos_compra_fornecedor_id_fkey FOREIGN KEY (fornecedor_id) REFERENCES public.fornecedores(id);


--
-- Name: pedidos_compra pedidos_compra_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pedidos_compra
    ADD CONSTRAINT pedidos_compra_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: servico_pecas_sugeridas servico_pecas_sugeridas_peca_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.servico_pecas_sugeridas
    ADD CONSTRAINT servico_pecas_sugeridas_peca_id_fkey FOREIGN KEY (peca_id) REFERENCES public.pecas(id) ON DELETE CASCADE;


--
-- Name: servico_pecas_sugeridas servico_pecas_sugeridas_servico_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.servico_pecas_sugeridas
    ADD CONSTRAINT servico_pecas_sugeridas_servico_id_fkey FOREIGN KEY (servico_id) REFERENCES public.servicos(id) ON DELETE CASCADE;


--
-- Name: servicos servicos_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.servicos
    ADD CONSTRAINT servicos_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: usuario_permissoes usuario_permissoes_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_permissoes
    ADD CONSTRAINT usuario_permissoes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;


--
-- Name: usuarios usuarios_oficina_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_oficina_id_fkey FOREIGN KEY (oficina_id) REFERENCES public.oficinas(id) ON DELETE CASCADE;


--
-- Name: veiculos veiculos_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.veiculos
    ADD CONSTRAINT veiculos_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.clientes(id) ON DELETE RESTRICT;


--
-- Name: agendamentos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.agendamentos ENABLE ROW LEVEL SECURITY;

--
-- Name: agendamentos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.agendamentos TO postgres USING (true) WITH CHECK (true);


--
-- Name: boxes api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.boxes TO postgres USING (true) WITH CHECK (true);


--
-- Name: checklist_itens api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.checklist_itens TO postgres USING (true) WITH CHECK (true);


--
-- Name: clientes api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.clientes TO postgres USING (true) WITH CHECK (true);


--
-- Name: despesas api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.despesas TO postgres USING (true) WITH CHECK (true);


--
-- Name: enderecos_clientes api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.enderecos_clientes TO postgres USING (true) WITH CHECK (true);


--
-- Name: estoque_movimentacoes api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.estoque_movimentacoes TO postgres USING (true) WITH CHECK (true);


--
-- Name: fornecedores api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.fornecedores TO postgres USING (true) WITH CHECK (true);


--
-- Name: notificacoes api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.notificacoes TO postgres USING (true) WITH CHECK (true);


--
-- Name: oficina_sequencias api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.oficina_sequencias TO postgres USING (true) WITH CHECK (true);


--
-- Name: oficinas api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.oficinas TO postgres USING (true) WITH CHECK (true);


--
-- Name: orcamento_aprovacoes api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.orcamento_aprovacoes TO postgres USING (true) WITH CHECK (true);


--
-- Name: orcamento_itens api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.orcamento_itens TO postgres USING (true) WITH CHECK (true);


--
-- Name: orcamentos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.orcamentos TO postgres USING (true) WITH CHECK (true);


--
-- Name: ordens_servico api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.ordens_servico TO postgres USING (true) WITH CHECK (true);


--
-- Name: os_checklist api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.os_checklist TO postgres USING (true) WITH CHECK (true);


--
-- Name: os_historico api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.os_historico TO postgres USING (true) WITH CHECK (true);


--
-- Name: os_mecanicos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.os_mecanicos TO postgres USING (true) WITH CHECK (true);


--
-- Name: os_pecas api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.os_pecas TO postgres USING (true) WITH CHECK (true);


--
-- Name: os_servicos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.os_servicos TO postgres USING (true) WITH CHECK (true);


--
-- Name: os_vistoria_danos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.os_vistoria_danos TO postgres USING (true) WITH CHECK (true);


--
-- Name: os_vistoria_fotos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.os_vistoria_fotos TO postgres USING (true) WITH CHECK (true);


--
-- Name: os_vistorias api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.os_vistorias TO postgres USING (true) WITH CHECK (true);


--
-- Name: pagamentos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.pagamentos TO postgres USING (true) WITH CHECK (true);


--
-- Name: pecas api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.pecas TO postgres USING (true) WITH CHECK (true);


--
-- Name: pedido_compra_itens api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.pedido_compra_itens TO postgres USING (true) WITH CHECK (true);


--
-- Name: pedidos_compra api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.pedidos_compra TO postgres USING (true) WITH CHECK (true);


--
-- Name: servico_pecas_sugeridas api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.servico_pecas_sugeridas TO postgres USING (true) WITH CHECK (true);


--
-- Name: servicos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.servicos TO postgres USING (true) WITH CHECK (true);


--
-- Name: status_ordens_servico api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.status_ordens_servico TO postgres USING (true) WITH CHECK (true);


--
-- Name: usuarios api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.usuarios TO postgres USING (true) WITH CHECK (true);


--
-- Name: veiculos api_role_full_access; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY api_role_full_access ON public.veiculos TO postgres USING (true) WITH CHECK (true);


--
-- Name: auditoria; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.auditoria ENABLE ROW LEVEL SECURITY;

--
-- Name: boxes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.boxes ENABLE ROW LEVEL SECURITY;

--
-- Name: checklist_itens; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.checklist_itens ENABLE ROW LEVEL SECURITY;

--
-- Name: clientes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.clientes ENABLE ROW LEVEL SECURITY;

--
-- Name: despesas; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.despesas ENABLE ROW LEVEL SECURITY;

--
-- Name: enderecos_clientes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.enderecos_clientes ENABLE ROW LEVEL SECURITY;

--
-- Name: estoque_movimentacoes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.estoque_movimentacoes ENABLE ROW LEVEL SECURITY;

--
-- Name: fornecedores; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.fornecedores ENABLE ROW LEVEL SECURITY;

--
-- Name: notificacoes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.notificacoes ENABLE ROW LEVEL SECURITY;

--
-- Name: oficina_sequencias; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.oficina_sequencias ENABLE ROW LEVEL SECURITY;

--
-- Name: oficinas; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.oficinas ENABLE ROW LEVEL SECURITY;

--
-- Name: orcamento_aprovacoes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.orcamento_aprovacoes ENABLE ROW LEVEL SECURITY;

--
-- Name: orcamento_itens; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.orcamento_itens ENABLE ROW LEVEL SECURITY;

--
-- Name: orcamentos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.orcamentos ENABLE ROW LEVEL SECURITY;

--
-- Name: ordens_servico; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.ordens_servico ENABLE ROW LEVEL SECURITY;

--
-- Name: os_checklist; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.os_checklist ENABLE ROW LEVEL SECURITY;

--
-- Name: os_historico; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.os_historico ENABLE ROW LEVEL SECURITY;

--
-- Name: os_mecanicos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.os_mecanicos ENABLE ROW LEVEL SECURITY;

--
-- Name: os_pecas; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.os_pecas ENABLE ROW LEVEL SECURITY;

--
-- Name: os_servicos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.os_servicos ENABLE ROW LEVEL SECURITY;

--
-- Name: os_vistoria_danos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.os_vistoria_danos ENABLE ROW LEVEL SECURITY;

--
-- Name: os_vistoria_fotos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.os_vistoria_fotos ENABLE ROW LEVEL SECURITY;

--
-- Name: os_vistorias; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.os_vistorias ENABLE ROW LEVEL SECURITY;

--
-- Name: pagamentos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.pagamentos ENABLE ROW LEVEL SECURITY;

--
-- Name: pecas; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.pecas ENABLE ROW LEVEL SECURITY;

--
-- Name: pedido_compra_itens; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.pedido_compra_itens ENABLE ROW LEVEL SECURITY;

--
-- Name: pedidos_compra; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.pedidos_compra ENABLE ROW LEVEL SECURITY;

--
-- Name: servico_pecas_sugeridas; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.servico_pecas_sugeridas ENABLE ROW LEVEL SECURITY;

--
-- Name: servicos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.servicos ENABLE ROW LEVEL SECURITY;

--
-- Name: status_ordens_servico; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.status_ordens_servico ENABLE ROW LEVEL SECURITY;

--
-- Name: usuario_permissoes; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.usuario_permissoes ENABLE ROW LEVEL SECURITY;

--
-- Name: usuarios; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ENABLE ROW LEVEL SECURITY;

--
-- Name: veiculos; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.veiculos ENABLE ROW LEVEL SECURITY;

--
-- PostgreSQL database dump complete
--


