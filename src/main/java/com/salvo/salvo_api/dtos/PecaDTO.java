package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Peca;
import java.math.BigDecimal;

public record PecaDTO(
    Long id,
    String codigo,
    String nome,
    String descricao,
    String fabricante,
    String oem,
    String categoria,
    String aplicacao,
    String unidade,
    BigDecimal precoCusto,
    BigDecimal precoVenda,
    BigDecimal estoqueAtual,
    BigDecimal estoqueMinimo,
    BigDecimal estoqueMaximo,
    BigDecimal estoqueReservado,
    BigDecimal estoqueDisponivel,
    String localizacao,
    Long fornecedorId,
    String observacao,
    String codigoBarras,
    Integer garantiaDias,
    String ncm,
    Boolean ativo
) {
    public static PecaDTO from(Peca p) {
        return new PecaDTO(
            p.getId(), p.getCodigo(), p.getNome(), p.getDescricao(),
            p.getFabricante(), p.getOem(), p.getCategoria(), p.getAplicacao(),
            p.getUnidade(), p.getPrecoCusto(), p.getPrecoVenda(),
            p.getEstoqueAtual(), p.getEstoqueMinimo(), p.getEstoqueMaximo(),
            p.getEstoqueReservado(), p.getEstoqueDisponivel(),
            p.getLocalizacao(), p.getFornecedorId(), p.getObservacao(),
            p.getCodigoBarras(), p.getGarantiaDias(), p.getNcm(), p.getAtivo()
        );
    }
}
