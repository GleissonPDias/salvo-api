package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;

public record AlterarStatusAgendamentoDTO(
    @NotBlank(message = "Status é obrigatório") String status
) {}
