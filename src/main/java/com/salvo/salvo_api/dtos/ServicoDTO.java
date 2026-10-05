package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Servico;
import java.math.BigDecimal;

public record ServicoDTO(Long id, String codigo, String nome, String descricao, BigDecimal precoBase) {
    public static ServicoDTO from(Servico s) {
        return new ServicoDTO(s.getId(), s.getCodigo(), s.getNome(), s.getDescricao(), s.getPrecoBase());
    }
}
