package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.PositiveOrZero;

public record AtualizarFornecedorDTO(
    String nome,
    String razaoSocial,
    String cnpj,
    String email,
    String telefone,
    String contato,
    @PositiveOrZero(message = "Prazo de entrega não pode ser negativo") Integer leadTimeDias,
    String endereco,
    String observacoes,
    Boolean ativo
) {}
