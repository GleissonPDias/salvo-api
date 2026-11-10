package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.PedidoCompra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PedidoCompraDTO(
    Long id,
    Integer numero,
    Long fornecedorId,
    String fornecedorNome,
    String status,
    LocalDate dataPedido,
    LocalDate previsaoEntrega,
    OffsetDateTime recebidoEm,
    String notaFiscal,
    String observacoes,
    Long criadoPor,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm,
    List<PedidoCompraItemDTO> itens,
    BigDecimal total
) {
    public static PedidoCompraDTO from(PedidoCompra p) {
        List<PedidoCompraItemDTO> itens = p.getItens().stream()
            .map(PedidoCompraItemDTO::from)
            .toList();
        BigDecimal total = itens.stream()
            .map(PedidoCompraItemDTO::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PedidoCompraDTO(
            p.getId(),
            p.getNumero(),
            p.getFornecedor() != null ? p.getFornecedor().getId() : null,
            p.getFornecedor() != null ? p.getFornecedor().getNome() : null,
            p.getStatus(),
            p.getDataPedido(),
            p.getPrevisaoEntrega(),
            p.getRecebidoEm(),
            p.getNotaFiscal(),
            p.getObservacoes(),
            p.getCriadoPor(),
            p.getCriadoEm(),
            p.getAtualizadoEm(),
            itens,
            total
        );
    }
}
