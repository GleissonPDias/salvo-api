package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.OsHistorico;
import java.time.OffsetDateTime;

public record OsHistoricoDTO(
    Long id,
    String statusAnterior,
    String statusNovo,
    String descricao,
    String usuario,
    OffsetDateTime criadoEm
) {
    public static OsHistoricoDTO from(OsHistorico h) {
        return new OsHistoricoDTO(
            h.getId(),
            h.getStatusAnterior() != null ? h.getStatusAnterior().getNome() : null,
            h.getStatusNovo() != null ? h.getStatusNovo().getNome() : null,
            h.getDescricao(),
            h.getUsuario() != null ? h.getUsuario().getNome() : null,
            h.getCriadoEm()
        );
    }
}
