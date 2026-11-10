package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.*;
import com.salvo.salvo_api.entities.Peca;
import com.salvo.salvo_api.entities.Servico;
import com.salvo.salvo_api.entities.ServicoPecaSugerida;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.PecaRepository;
import com.salvo.salvo_api.repositories.ServicoPecaSugeridaRepository;
import com.salvo.salvo_api.repositories.ServicoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

/**
 * Catálogo de serviços/peças e movimentações de estoque.
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final ServicoPecaSugeridaRepository sugeridaRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    /* ------------------------------------------------------------------ */
    /* Serviços                                                            */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public List<ServicoDTO> listarServicos(String q) {
        Long oficina = oficinaContexto.getOficinaId();
        return servicoRepository.buscar(oficina, q == null ? "" : q.trim())
            .stream().map(ServicoDTO::from).toList();
    }

    @Transactional
    public ServicoDTO criarServico(CriarServicoDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();

        Servico servico = new Servico();
        servico.setOficina(em.getReference(com.salvo.salvo_api.entities.Oficina.class, oficina));
        servico.setCodigo(codigoOuGerado(dto.codigo(), "SV", servicoRepository.findByOficinaIdOrderByCodigo(oficina)));
        servico.setNome(dto.nome().trim());
        servico.setDescricao(dto.descricao());
        servico.setCategoria(dto.categoria());
        servico.setDetalhe(dto.detalhe());
        servico.setTempoEstimadoHoras(dto.tempoEstimadoHoras());
        servico.setGarantiaTipo(dto.garantiaTipo());
        servico.setPrecoBase(dto.precoBase() != null ? dto.precoBase() : BigDecimal.ZERO);
        servico.setAtivo(dto.ativo() == null || dto.ativo());

        return ServicoDTO.from(servicoRepository.save(servico));
    }

    @Transactional
    public ServicoDTO atualizarServico(Long id, AtualizarServicoDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        Servico servico = servicoRepository.findByIdAndOficinaId(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + id));

        if (dto.codigo() != null && !dto.codigo().isBlank()) servico.setCodigo(dto.codigo().trim());
        if (dto.nome() != null && !dto.nome().isBlank()) servico.setNome(dto.nome().trim());
        if (dto.descricao() != null) servico.setDescricao(dto.descricao());
        if (dto.categoria() != null) servico.setCategoria(dto.categoria());
        if (dto.detalhe() != null) servico.setDetalhe(dto.detalhe());
        if (dto.tempoEstimadoHoras() != null) servico.setTempoEstimadoHoras(dto.tempoEstimadoHoras());
        if (dto.garantiaTipo() != null) servico.setGarantiaTipo(dto.garantiaTipo());
        if (dto.precoBase() != null) servico.setPrecoBase(dto.precoBase());
        if (dto.ativo() != null) servico.setAtivo(dto.ativo());

        return ServicoDTO.from(servicoRepository.save(servico));
    }

    @Transactional
    public void desativarServico(Long id) {
        Long oficina = oficinaContexto.getOficinaId();
        Servico servico = servicoRepository.findByIdAndOficinaId(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + id));
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }

    /* ------------------------------------------------------------------ */
    /* Peças sugeridas por serviço                                         */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public List<PecaDTO> listarPecasSugeridas(Long servicoId) {
        Long oficina = oficinaContexto.getOficinaId();
        servicoRepository.findByIdAndOficinaId(servicoId, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + servicoId));

        List<Long> pecaIds = sugeridaRepository.findByServicoId(servicoId).stream()
            .map(ServicoPecaSugerida::getPecaId)
            .toList();
        if (pecaIds.isEmpty()) return List.of();

        return pecaRepository.findByOficinaIdAndIdInOrderByCodigo(oficina, pecaIds)
            .stream().map(PecaDTO::from).toList();
    }

    @Transactional
    public List<PecaDTO> substituirPecasSugeridas(Long servicoId, List<Long> pecaIds) {
        Long oficina = oficinaContexto.getOficinaId();
        servicoRepository.findByIdAndOficinaId(servicoId, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + servicoId));

        List<Long> distinct = pecaIds == null
            ? List.of()
            : pecaIds.stream().filter(java.util.Objects::nonNull).distinct().toList();

        for (Long pecaId : distinct) {
            pecaRepository.findByIdAndOficinaId(pecaId, oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + pecaId));
        }

        sugeridaRepository.deleteByServicoId(servicoId);
        sugeridaRepository.flush();

        if (!distinct.isEmpty()) {
            sugeridaRepository.saveAll(distinct.stream().map(pecaId -> {
                ServicoPecaSugerida sugestao = new ServicoPecaSugerida();
                sugestao.setServicoId(servicoId);
                sugestao.setPecaId(pecaId);
                return sugestao;
            }).toList());
        }

        return listarPecasSugeridas(servicoId);
    }

    /* ------------------------------------------------------------------ */
    /* Peças                                                               */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public List<PecaDTO> listarPecas(String q) {
        Long oficina = oficinaContexto.getOficinaId();
        return pecaRepository.buscar(oficina, q == null ? "" : q.trim())
            .stream().map(PecaDTO::from).toList();
    }

    @Transactional
    public PecaDTO criarPeca(CriarPecaDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();

        Peca peca = new Peca();
        peca.setOficina(em.getReference(com.salvo.salvo_api.entities.Oficina.class, oficina));
        peca.setCodigo(codigoOuGerado(dto.codigo(), "PC", pecaRepository.findByOficinaIdOrderByCodigo(oficina)));
        peca.setNome(dto.nome().trim());
        peca.setDescricao(dto.descricao());
        peca.setFabricante(dto.fabricante());
        peca.setOem(dto.oem());
        peca.setCategoria(dto.categoria());
        peca.setAplicacao(dto.aplicacao());
        peca.setUnidade(dto.unidade() != null ? dto.unidade() : "un");
        peca.setPrecoCusto(dto.precoCusto() != null ? dto.precoCusto() : BigDecimal.ZERO);
        peca.setPrecoVenda(dto.precoVenda() != null ? dto.precoVenda() : BigDecimal.ZERO);
        peca.setEstoqueAtual(dto.estoqueAtual() != null ? dto.estoqueAtual() : BigDecimal.ZERO);
        peca.setEstoqueMinimo(dto.estoqueMinimo() != null ? dto.estoqueMinimo() : BigDecimal.ZERO);
        peca.setEstoqueMaximo(dto.estoqueMaximo() != null ? dto.estoqueMaximo() : BigDecimal.ZERO);
        peca.setEstoqueReservado(BigDecimal.ZERO);
        peca.setLocalizacao(dto.localizacao());
        peca.setFornecedorId(dto.fornecedorId());
        peca.setObservacao(dto.observacao());
        peca.setCodigoBarras(dto.codigoBarras());
        peca.setGarantiaDias(dto.garantiaDias());
        peca.setNcm(dto.ncm());
        peca.setAtivo(dto.ativo() == null || dto.ativo());

        Peca salva = pecaRepository.save(peca);
        em.flush();
        em.refresh(salva);
        return PecaDTO.from(salva);
    }

    @Transactional
    public PecaDTO atualizarPeca(Long id, AtualizarPecaDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        Peca peca = pecaRepository.findByIdAndOficinaId(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + id));

        if (dto.codigo() != null && !dto.codigo().isBlank()) peca.setCodigo(dto.codigo().trim());
        if (dto.nome() != null && !dto.nome().isBlank()) peca.setNome(dto.nome().trim());
        if (dto.descricao() != null) peca.setDescricao(dto.descricao());
        if (dto.fabricante() != null) peca.setFabricante(dto.fabricante());
        if (dto.oem() != null) peca.setOem(dto.oem());
        if (dto.categoria() != null) peca.setCategoria(dto.categoria());
        if (dto.aplicacao() != null) peca.setAplicacao(dto.aplicacao());
        if (dto.unidade() != null) peca.setUnidade(dto.unidade());
        if (dto.precoCusto() != null) peca.setPrecoCusto(dto.precoCusto());
        if (dto.precoVenda() != null) peca.setPrecoVenda(dto.precoVenda());
        if (dto.estoqueMinimo() != null) peca.setEstoqueMinimo(dto.estoqueMinimo());
        if (dto.estoqueMaximo() != null) peca.setEstoqueMaximo(dto.estoqueMaximo());
        if (dto.localizacao() != null) peca.setLocalizacao(dto.localizacao());
        if (dto.fornecedorId() != null) peca.setFornecedorId(dto.fornecedorId());
        if (dto.observacao() != null) peca.setObservacao(dto.observacao());
        if (dto.codigoBarras() != null) peca.setCodigoBarras(dto.codigoBarras());
        if (dto.garantiaDias() != null) peca.setGarantiaDias(dto.garantiaDias());
        if (dto.ncm() != null) peca.setNcm(dto.ncm());
        if (dto.ativo() != null) peca.setAtivo(dto.ativo());

        Peca salva = pecaRepository.save(peca);
        em.flush();
        em.refresh(salva);
        return PecaDTO.from(salva);
    }

    @Transactional
    public void desativarPeca(Long id) {
        Long oficina = oficinaContexto.getOficinaId();
        Peca peca = pecaRepository.findByIdAndOficinaId(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + id));
        peca.setAtivo(false);
        pecaRepository.save(peca);
    }

    /* ------------------------------------------------------------------ */
    /* Estoque                                                             */
    /* ------------------------------------------------------------------ */

    @Transactional
    public MovimentacaoEstoqueRespostaDTO movimentar(Long pecaId, MovimentacaoEstoqueDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        Long usuario = oficinaContexto.getUsuarioId();

        Peca peca = pecaRepository.findByIdAndOficinaId(pecaId, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + pecaId));

        String tipo = dto.tipo().trim().toLowerCase(Locale.ROOT);
        BigDecimal saldoAnterior = peca.getEstoqueAtual() != null ? peca.getEstoqueAtual() : BigDecimal.ZERO;
        BigDecimal quantidade;
        short direcao;
        String rotulo;

        switch (tipo) {
            case "entrada" -> {
                quantidade = exigirPositivo(dto.quantidade(), "Entrada");
                direcao = 1;
                rotulo = "ENTRADA";
            }
            case "devolucao" -> {
                quantidade = exigirPositivo(dto.quantidade(), "Devolução");
                direcao = 1;
                rotulo = "DEVOLUCAO";
            }
            case "saida" -> {
                quantidade = exigirPositivo(dto.quantidade(), "Saída");
                direcao = -1;
                rotulo = "SAIDA";
            }
            case "ajuste" -> {
                BigDecimal novoSaldo = dto.quantidade() != null ? dto.quantidade() : BigDecimal.ZERO;
                BigDecimal delta = novoSaldo.subtract(saldoAnterior);
                if (delta.compareTo(BigDecimal.ZERO) == 0) {
                    throw new RegraDeNegocioException(
                        "O saldo informado já é o estoque atual da peça (" + saldoAnterior.stripTrailingZeros().toPlainString() + ")."
                    );
                }
                quantidade = delta.abs();
                direcao = (short) (delta.signum() > 0 ? 1 : -1);
                rotulo = "AJUSTE";
            }
            default -> throw new RegraDeNegocioException(
                "Tipo de movimentação inválido: " + dto.tipo() + ". Use entrada, saida, ajuste ou devolucao."
            );
        }

        validarFornecedor(oficina, dto.fornecedorId());
        validarPedidoCompra(oficina, dto.pedidoCompraId());

        String udt = tipoColunaMovimentacao();
        String label = rotuloEnum(udt, rotulo);
        String tipoSql = udt.matches("[A-Za-z0-9_]+") ? udt : "varchar";

        Number id = (Number) em.createNativeQuery(
                "INSERT INTO public.estoque_movimentacoes " +
                "(oficina_id, peca_id, tipo, quantidade, direcao, custo_unitario, documento, observacao, " +
                " usuario_id, fornecedor_id, pedido_compra_id) " +
                "VALUES (:oficina, :peca, CAST(:tipo AS " + tipoSql + "), :quantidade, :direcao, :custo, " +
                " :documento, :observacao, :usuario, :fornecedor, :pedido) " +
                "RETURNING id")
            .setParameter("oficina", oficina)
            .setParameter("peca", pecaId)
            .setParameter("tipo", label)
            .setParameter("quantidade", quantidade)
            .setParameter("direcao", direcao)
            .setParameter("custo", dto.custoUnitario())
            .setParameter("documento", dto.documento())
            .setParameter("observacao", dto.observacao())
            .setParameter("usuario", usuario)
            .setParameter("fornecedor", dto.fornecedorId())
            .setParameter("pedido", dto.pedidoCompraId())
            .getSingleResult();

        // Entrada com custo informado atualiza o custo médio ponderado.
        // A atualização é feita por SQL direto e SOMENTE na coluna preco_custo:
        // o estoque_atual é alterado pelo trigger BEFORE INSERT da movimentação.
        // Se a entidade gerenciada fosse salva aqui, o Hibernate reescreveria
        // estoque_atual com o valor antigo e desfaria o efeito do trigger.
        if ("entrada".equals(tipo) && dto.custoUnitario() != null && dto.custoUnitario().signum() > 0) {
            BigDecimal totalAtual = saldoAnterior.multiply(peca.getPrecoCusto() != null ? peca.getPrecoCusto() : BigDecimal.ZERO);
            BigDecimal totalNovo = quantidade.multiply(dto.custoUnitario());
            BigDecimal novoSaldo = saldoAnterior.add(quantidade);
            if (novoSaldo.signum() > 0) {
                BigDecimal custoMedio = totalAtual.add(totalNovo)
                    .divide(novoSaldo, 4, RoundingMode.HALF_UP);
                em.createNativeQuery("UPDATE public.pecas SET preco_custo = :custo WHERE id = :id")
                    .setParameter("custo", custoMedio)
                    .setParameter("id", pecaId)
                    .executeUpdate();
            }
        }

        em.flush();
        em.refresh(peca);
        BigDecimal saldoAtual = peca.getEstoqueAtual();

        return new MovimentacaoEstoqueRespostaDTO(
            id.longValue(),
            pecaId,
            peca.getCodigo(),
            tipo,
            quantidade,
            direcao,
            saldoAnterior,
            saldoAtual,
            saldoAtual,
            dto.custoUnitario(),
            dto.documento(),
            dto.observacao(),
            OffsetDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueRespostaDTO> listarMovimentacoes(Long pecaId, int limite) {
        Long oficina = oficinaContexto.getOficinaId();
        int limit = Math.min(Math.max(limite, 1), 500);

        String sql = "SELECT m.id, m.peca_id, p.codigo, m.tipo::text, m.quantidade, m.direcao, " +
            "m.saldo_anterior, m.saldo_posterior, p.estoque_atual, m.custo_unitario, " +
            "m.documento, m.observacao, m.criado_em " +
            "FROM public.estoque_movimentacoes m " +
            "JOIN public.pecas p ON p.id = m.peca_id " +
            "WHERE m.oficina_id = :oficina " +
            (pecaId != null ? "AND m.peca_id = :peca " : "") +
            "ORDER BY m.criado_em DESC, m.id DESC LIMIT :limite";

        var query = em.createNativeQuery(sql).setParameter("oficina", oficina);
        if (pecaId != null) query.setParameter("peca", pecaId);
        query.setParameter("limite", limit);

        List<?> rows = query.getResultList();
        return rows.stream().map(row -> {
            Object[] r = (Object[]) row;
            return new MovimentacaoEstoqueRespostaDTO(
                numero(r[0]),
                numero(r[1]),
                texto(r[2]),
                texto(r[3]),
                decimal(r[4]),
                (short) numero(r[5]).shortValue(),
                decimal(r[6]),
                decimal(r[7]),
                decimal(r[8]),
                decimal(r[9]),
                texto(r[10]),
                texto(r[11]),
                data(r[12])
            );
        }).toList();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private BigDecimal exigirPositivo(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) {
            throw new RegraDeNegocioException(label + " exige quantidade maior que zero.");
        }
        return value;
    }

    private String tipoColunaMovimentacao() {
        try {
            Object value = em.createNativeQuery(
                    "SELECT udt_name FROM information_schema.columns " +
                    "WHERE table_schema = 'public' AND table_name = 'estoque_movimentacoes' " +
                    "AND column_name = 'tipo'")
                .getSingleResult();
            return value != null ? String.valueOf(value) : "varchar";
        } catch (Exception ex) {
            return "varchar";
        }
    }

    @SuppressWarnings("unchecked")
    private String rotuloEnum(String udt, String esperado) {
        try {
            List<String> labels = em.createNativeQuery(
                    "SELECT e.enumlabel FROM pg_type t " +
                    "JOIN pg_enum e ON e.enumtypid = t.oid " +
                    "WHERE t.typname = :tipo ORDER BY e.enumsortorder")
                .setParameter("tipo", udt)
                .getResultList();
            String chave = normalizar(esperado);
            for (String label : labels) {
                String atual = normalizar(label);
                if (atual.equals(chave)
                        || (chave.equals("DEVOLUCAO") && atual.contains("DEVOLU"))
                        || (chave.equals("ENTRADA") && atual.contains("ENTRADA"))
                        || (chave.equals("SAIDA") && atual.contains("SAIDA"))
                        || (chave.equals("AJUSTE") && atual.contains("AJUSTE"))) {
                    return label;
                }
            }
        } catch (Exception ignored) {
            // coluna varchar (ou sem permissão em pg_enum): envia o rótulo padrão.
        }
        return esperado;
    }

    private void validarFornecedor(Long oficina, Long fornecedorId) {
        if (fornecedorId == null) return;
        Number count = (Number) em.createNativeQuery(
                "SELECT count(*) FROM public.fornecedores WHERE id = :id AND oficina_id = :oficina")
            .setParameter("id", fornecedorId)
            .setParameter("oficina", oficina)
            .getSingleResult();
        if (count.longValue() == 0) {
            throw new RecursoNaoEncontradoException("Fornecedor não encontrado: " + fornecedorId);
        }
    }

    private void validarPedidoCompra(Long oficina, Long pedidoId) {
        if (pedidoId == null) return;
        Number count = (Number) em.createNativeQuery(
                "SELECT count(*) FROM public.pedidos_compra WHERE id = :id AND oficina_id = :oficina")
            .setParameter("id", pedidoId)
            .setParameter("oficina", oficina)
            .getSingleResult();
        if (count.longValue() == 0) {
            throw new RecursoNaoEncontradoException("Pedido de compra não encontrado: " + pedidoId);
        }
    }

    private String codigoOuGerado(String codigo, String prefixo, List<?> existentes) {
        if (codigo != null && !codigo.isBlank()) return codigo.trim().toUpperCase(Locale.ROOT);

        int maior = 0;
        for (Object item : existentes) {
            String atual = item instanceof Servico s ? s.getCodigo() : item instanceof Peca p ? p.getCodigo() : null;
            if (atual == null) continue;
            String digits = atual.replaceAll("\\D", "");
            if (!digits.isBlank()) {
                try {
                    maior = Math.max(maior, Integer.parseInt(digits));
                } catch (NumberFormatException ignored) {
                    // código não numérico: ignora no cálculo
                }
            }
        }
        return prefixo + "-" + String.format("%03d", maior + 1);
    }

    private String normalizar(String value) {
        return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT)
            .trim();
    }

    private Long numero(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal b) return b;
        return new BigDecimal(String.valueOf(value));
    }

    private String texto(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private OffsetDateTime data(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime odt) return odt;
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        if (value instanceof Instant instant) return instant.atOffset(ZoneOffset.UTC);
        return OffsetDateTime.parse(String.valueOf(value));
    }
}
