package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Orcamento;
import com.salvo.salvo_api.entities.OrcamentoItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record OrcamentoDTO(
    Long id,
    Integer numero,
    Long clienteId,
    Long veiculoId,
    Long criadoPor,
    String criadoPorNome,
    Long ordemServicoId,
    String status,
    LocalDate validade,
    String observacoes,
    BigDecimal desconto,
    String descontoTipo,
    String titulo,
    String tipo,
    Short validadeDias,
    String canalEnvio,
    OffsetDateTime emitidoEm,
    OffsetDateTime aprovadoEm,
    OffsetDateTime rejeitadoEm,
    String motivoRejeicao,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm,
    List<OrcamentoItemDTO> itens
) {
    public static OrcamentoDTO from(Orcamento o) {
        return from(o, null);
    }

    public static OrcamentoDTO from(Orcamento o, String criadoPorNome) {
        return new OrcamentoDTO(
            o.getId(),
            o.getNumero(),
            o.getClienteId(),
            o.getVeiculoId(),
            o.getCriadoPor(),
            criadoPorNome,
            o.getOrdemServicoId(),
            o.getStatus().name(),
            o.getValidade(),
            o.getObservacoes(),
            o.getDesconto(),
            o.getDescontoTipo(),
            o.getTitulo(),
            o.getTipo(),
            o.getValidadeDias(),
            o.getCanalEnvio(),
            o.getEmitidoEm(),
            o.getAprovadoEm(),
            o.getRejeitadoEm(),
            o.getMotivoRejeicao(),
            o.getCriadoEm(),
            o.getAtualizadoEm(),
            o.getItens().stream().map(OrcamentoItemDTO::from).toList()
        );
    }
}
