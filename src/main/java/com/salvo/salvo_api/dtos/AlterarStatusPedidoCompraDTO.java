package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;

public record AlterarStatusPedidoCompraDTO(
    @NotBlank(message = "Status é obrigatório") String status
) {}
