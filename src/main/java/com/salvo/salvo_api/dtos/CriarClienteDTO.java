package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;

public record CriarClienteDTO(
    @NotBlank(message = "Nome é obrigatório") String nome,
    @NotBlank(message = "CPF/CNPJ é obrigatório") String cpfCnpj,
    String email,
    @NotBlank(message = "Telefone é obrigatório") String telefone,
    String telefoneSecundario,
    String classificacao
) {}
