package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Veiculo;

public record VeiculoDTO(
    Long id,
    Long clienteId,
    String clienteNome,
    Long oficinaId,
    String placa,
    String chassi,
    String marca,
    String modelo,
    String versao,
    Short anoFabricacao,
    Short anoModelo,
    String cor,
    String combustivel,
    String motorizacao,
    String transmissao,
    Integer kmAtual,
    String fotoUrl,
    String categoria,
    Boolean rascunho,
    Boolean ativo
) {
    public static VeiculoDTO from(Veiculo v) {
        return new VeiculoDTO(
            v.getId(),
            v.getCliente().getId(),
            v.getCliente().getNome(),
            v.getOficina() != null ? v.getOficina().getId() : null,
            v.getPlaca(), v.getChassi(),
            v.getMarca(), v.getModelo(), v.getVersao(),
            v.getAnoFabricacao(), v.getAnoModelo(),
            v.getCor(), v.getCombustivel(),
            v.getMotorizacao(), v.getTransmissao(),
            v.getKmAtual(), v.getFotoUrl(), v.getCategoria(),
            v.getRascunho(), v.getAtivo()
        );
    }
}
