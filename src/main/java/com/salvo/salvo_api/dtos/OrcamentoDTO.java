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
    String clienteNome,
    String veiculoPlaca,
    String veiculoModelo,
    Long criadoPor,
    String criadoPorNome,
    Long vendedorId,
    String vendedorNome,
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
        return from(o, null, null, null, null, null);
    }

    public static OrcamentoDTO from(Orcamento o, String criadoPorNome) {
        return from(o, criadoPorNome, null, null, null, null);
    }

    public static OrcamentoDTO from(Orcamento o, String criadoPorNome, String vendedorNome) {
        return from(o, criadoPorNome, vendedorNome, null, null, null);
    }

    public static OrcamentoDTO from(Orcamento o, String criadoPorNome, String vendedorNome, String clienteNome, String veiculoPlaca, String veiculoModelo) {
        return new OrcamentoDTO(
            o.getId(),
            o.getNumero(),
            o.getClienteId(),
            o.getVeiculoId(),
            clienteNome,
            veiculoPlaca,
            veiculoModelo,
            o.getCriadoPor(),
            criadoPorNome,
            o.getVendedorId(),
            vendedorNome,
            o.getOrdemServicoId(),
            o.getStatus(),
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
