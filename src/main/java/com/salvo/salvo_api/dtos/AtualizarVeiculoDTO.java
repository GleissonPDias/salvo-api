package com.salvo.salvo_api.dtos;

public record AtualizarVeiculoDTO(
    String cor,
    String combustivel,
    String motorizacao,
    String transmissao,
    Integer kmAtual,
    String versao,
    String fotoUrl
) {}
