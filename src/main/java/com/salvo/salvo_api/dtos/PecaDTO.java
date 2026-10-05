package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Peca;
import java.math.BigDecimal;

public record PecaDTO(Long id, String codigo, String nome, String fabricante,
                      BigDecimal precoVenda, BigDecimal estoqueAtual) {
    public static PecaDTO from(Peca p) {
        return new PecaDTO(p.getId(), p.getCodigo(), p.getNome(), p.getFabricante(),
            p.getPrecoVenda(), p.getEstoqueAtual());
    }
}
