package com.salvo.salvo_api.dtos;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

public record AtualizarPedidoCompraDTO(
    Long fornecedorId,
    LocalDate previsaoEntrega,
    String notaFiscal,
    String observacoes,
    List<@Valid ItemPedidoCompraDTO> itens
) {}
