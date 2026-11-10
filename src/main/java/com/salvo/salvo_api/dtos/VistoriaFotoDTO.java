package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.OsVistoriaFoto;
import jakarta.validation.constraints.NotBlank;

public record VistoriaFotoDTO(
    Long id,
    @NotBlank(message = "URL da foto é obrigatória") String url,
    String storagePath,
    String legenda
) {
    public static VistoriaFotoDTO from(OsVistoriaFoto f) {
        return new VistoriaFotoDTO(f.getId(), f.getUrl(), f.getStoragePath(), f.getLegenda());
    }
}
