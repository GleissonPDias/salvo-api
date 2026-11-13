package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.OsVistoria;

import java.time.OffsetDateTime;
import java.util.List;

public record OsVistoriaDTO(
    Long id,
    Long ordemServicoId,
    Boolean clienteCiente,
    String pertences,
    String observacoes,
    OffsetDateTime realizadaEm,
    Long realizadaPor,
    List<VistoriaDanoDTO> danos
) {
    public static OsVistoriaDTO from(OsVistoria v) {
        return new OsVistoriaDTO(
            v.getId(),
            v.getOrdemServico() != null ? v.getOrdemServico().getId() : null,
            v.getClienteCiente(),
            v.getPertences(),
            v.getObservacoes(),
            v.getRealizadaEm(),
            v.getRealizadaPor(),
            v.getDanos().stream().map(VistoriaDanoDTO::from).toList()
        );
    }

    public static OsVistoriaDTO vazia(Long ordemServicoId) {
        return new OsVistoriaDTO(
            null, ordemServicoId, false, null, null, null, null, List.of()
        );
    }
}
