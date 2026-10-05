package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;

public record AlterarStatusOsDTO(
    @NotBlank(message = "Status é obrigatório") String status,
    String observacao
) {}
