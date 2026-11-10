package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarPedidoCompraDTO;
import com.salvo.salvo_api.dtos.CriarPedidoCompraDTO;
import com.salvo.salvo_api.dtos.ItemPedidoCompraDTO;
import com.salvo.salvo_api.dtos.MovimentacaoEstoqueDTO;
import com.salvo.salvo_api.dtos.PedidoCompraDTO;
import com.salvo.salvo_api.entities.Fornecedor;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.entities.Peca;
import com.salvo.salvo_api.entities.PedidoCompra;
import com.salvo.salvo_api.entities.PedidoCompraItem;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.FornecedorRepository;
import com.salvo.salvo_api.repositories.PecaRepository;
import com.salvo.salvo_api.repositories.PedidoCompraRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PedidoCompraService {

    private static final Set<String> STATUS_VALIDOS =
        Set.of("RASCUNHO", "ENVIADO", "RECEBIDO", "CANCELADO");

    private final PedidoCompraRepository pedidoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final PecaRepository pecaRepository;
    private final CatalogoService catalogoService;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<PedidoCompraDTO> listar(String status) {
        Long oficina = oficinaContexto.getOficinaId();
        List<PedidoCompra> pedidos = status == null || status.isBlank()
            ? pedidoRepository.listar(oficina)
            : pedidoRepository.listarPorStatus(oficina, normalizarStatus(status));
        return pedidos.stream().map(PedidoCompraDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoCompraDTO buscar(Long id) {
        return PedidoCompraDTO.from(exigirPedido(id));
    }

    @Transactional
    public PedidoCompraDTO criar(CriarPedidoCompraDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Long usuarioId = oficinaContexto.getUsuarioId();

        PedidoCompra pedido = new PedidoCompra();
        pedido.setOficina(em.getReference(Oficina.class, oficinaId));
        pedido.setNumero(proximoNumero(oficinaId));
        pedido.setFornecedor(exigirFornecedor(dto.fornecedorId(), oficinaId));
        pedido.setStatus("RASCUNHO");
        pedido.setDataPedido(LocalDate.now());
        pedido.setPrevisaoEntrega(dto.previsaoEntrega());
        pedido.setNotaFiscal(limpar(dto.notaFiscal()));
        pedido.setObservacoes(limpar(dto.observacoes()));
        pedido.setCriadoPor(usuarioId);

        aplicarItens(pedido, dto.itens(), oficinaId);

        return PedidoCompraDTO.from(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoCompraDTO atualizar(Long id, AtualizarPedidoCompraDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        PedidoCompra pedido = exigirPedido(id);

        if (dto.fornecedorId() != null) {
            pedido.setFornecedor(exigirFornecedor(dto.fornecedorId(), oficinaId));
        }
        if (dto.previsaoEntrega() != null) pedido.setPrevisaoEntrega(dto.previsaoEntrega());
        if (dto.notaFiscal() != null) pedido.setNotaFiscal(limpar(dto.notaFiscal()));
        if (dto.observacoes() != null) pedido.setObservacoes(limpar(dto.observacoes()));

        if (dto.itens() != null) {
            if ("RECEBIDO".equals(pedido.getStatus())) {
                throw new RegraDeNegocioException("Pedido já recebido: os itens não podem ser alterados.");
            }
            pedido.getItens().clear();
            aplicarItens(pedido, dto.itens(), oficinaId);
        }

        return PedidoCompraDTO.from(pedidoRepository.save(pedido));
    }

    /**
     * Muda o status. Ao entrar em RECEBIDO, dá entrada no estoque das peças
     * ainda pendentes (quantidade - quantidade recebida) e atualiza o custo médio.
     */
    @Transactional
    public PedidoCompraDTO alterarStatus(Long id, String status) {
        PedidoCompra pedido = exigirPedido(id);
        String novo = normalizarStatus(status);

        if (novo.equals(pedido.getStatus())) {
            return PedidoCompraDTO.from(pedido);
        }
        if ("RECEBIDO".equals(pedido.getStatus())) {
            throw new RegraDeNegocioException("Pedido já recebido: o status não pode ser alterado.");
        }
        if ("CANCELADO".equals(pedido.getStatus())) {
            throw new RegraDeNegocioException("Pedido cancelado: o status não pode ser alterado.");
        }

        if ("RECEBIDO".equals(novo)) {
            receber(pedido);
        } else if ("CANCELADO".equals(novo)) {
            pedido.setStatus("CANCELADO");
        } else {
            pedido.setStatus(novo);
        }

        return PedidoCompraDTO.from(pedidoRepository.save(pedido));
    }

    @Transactional
    public void excluir(Long id) {
        PedidoCompra pedido = exigirPedido(id);
        if (!Set.of("RASCUNHO", "CANCELADO").contains(pedido.getStatus())) {
            throw new RegraDeNegocioException(
                "Só é possível excluir pedidos em rascunho ou cancelados."
            );
        }
        pedidoRepository.delete(pedido);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private void receber(PedidoCompra pedido) {
        if (pedido.getItens().isEmpty()) {
            throw new RegraDeNegocioException("Pedido sem itens: não há o que receber.");
        }

        Long oficina = pedido.getOficina().getId();
        Long fornecedorId = pedido.getFornecedor().getId();
        String documento = "Pedido de compra #" + pedido.getNumero();

        for (PedidoCompraItem item : pedido.getItens()) {
            BigDecimal pendente = item.getQuantidade().subtract(item.getQuantidadeRecebida());
            if (pendente.signum() <= 0 || item.getPecaId() == null) continue;

            pecaRepository.findByIdAndOficinaId(item.getPecaId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Peça do pedido não encontrada: " + item.getPecaId()
                ));

            catalogoService.movimentar(
                item.getPecaId(),
                new MovimentacaoEstoqueDTO(
                    "entrada",
                    pendente,
                    item.getCustoUnitario(),
                    fornecedorId,
                    pedido.getId(),
                    documento,
                    "Recebimento do pedido de compra"
                )
            );
            item.setQuantidadeRecebida(item.getQuantidade());
        }

        pedido.setStatus("RECEBIDO");
        pedido.setRecebidoEm(OffsetDateTime.now());
    }

    private void aplicarItens(PedidoCompra pedido, List<ItemPedidoCompraDTO> itens, Long oficinaId) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraDeNegocioException("Informe ao menos um item no pedido de compra.");
        }

        for (ItemPedidoCompraDTO dto : itens) {
            Peca peca = null;
            if (dto.pecaId() != null) {
                peca = pecaRepository.findByIdAndOficinaId(dto.pecaId(), oficinaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Peça não encontrada: " + dto.pecaId()
                    ));
            }

            PedidoCompraItem item = new PedidoCompraItem();
            item.setPedido(pedido);
            item.setPecaId(dto.pecaId());
            item.setDescricao(dto.descricao() == null || dto.descricao().isBlank()
                ? (peca != null ? peca.getNome() : "Item")
                : dto.descricao().trim());
            item.setQuantidade(dto.quantidade());
            item.setCustoUnitario(dto.custoUnitario() != null ? dto.custoUnitario() : BigDecimal.ZERO);
            item.setQuantidadeRecebida(BigDecimal.ZERO);
            pedido.getItens().add(item);
        }
    }

    private Integer proximoNumero(Long oficinaId) {
        Number numero = (Number) em.createNativeQuery(
                "SELECT public.fn_proximo_numero_pedido_compra(:oficina)")
            .setParameter("oficina", oficinaId)
            .getSingleResult();
        return numero.intValue();
    }

    private PedidoCompra exigirPedido(Long id) {
        Long oficina = oficinaContexto.getOficinaId();
        return pedidoRepository.buscar(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido de compra não encontrado: " + id));
    }

    private Fornecedor exigirFornecedor(Long id, Long oficinaId) {
        if (id == null) {
            throw new RegraDeNegocioException("Fornecedor é obrigatório.");
        }
        return fornecedorRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
    }

    private String normalizarStatus(String status) {
        String valor = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_VALIDOS.contains(valor)) {
            throw new RegraDeNegocioException(
                "Status inválido: " + status + ". Use RASCUNHO, ENVIADO, RECEBIDO ou CANCELADO."
            );
        }
        return valor;
    }

    private String limpar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
