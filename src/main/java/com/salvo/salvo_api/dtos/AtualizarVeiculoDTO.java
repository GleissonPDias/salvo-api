package com.salvo.salvo_api.dtos;

public record AtualizarVeiculoDTO(
    String placa,
    String marca,
    String modelo,
    String cor,
    String combustivel,
    String motorizacao,
    String transmissao,
    Integer kmAtual,
    String versao,
    String fotoUrl,
    String categoria,
    String chassi,
    Short anoFabricacao,
    Short anoModelo,
    Boolean rascunho,
    Boolean ativo
) {}