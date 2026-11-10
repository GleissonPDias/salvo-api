package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;

public record AlterarStatusFinanceiroDTO(
    @NotBlank(message = "Status é obrigatório") String status
) {}
