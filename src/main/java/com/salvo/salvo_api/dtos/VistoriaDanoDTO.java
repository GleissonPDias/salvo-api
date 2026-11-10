package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.OsVistoriaDano;
import jakarta.validation.constraints.NotBlank;

public record VistoriaDanoDTO(
    Long id,
    @NotBlank(message = "Área do dano é obrigatória") String area,
    @NotBlank(message = "Tipo do dano é obrigatório") String tipo,
    String observacao
) {
    public static VistoriaDanoDTO from(OsVistoriaDano d) {
        return new VistoriaDanoDTO(d.getId(), d.getArea(), d.getTipo(), d.getObservacao());
    }
}
