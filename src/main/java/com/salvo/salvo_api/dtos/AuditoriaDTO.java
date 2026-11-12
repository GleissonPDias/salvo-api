package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Auditoria;

import java.time.OffsetDateTime;

public record AuditoriaDTO(
    Long id,
    Long usuarioId,
    String usuarioNome,
    String acao,
    String modulo,
    String entidade,
    String entidadeId,
    String metodo,
    String caminho,
    Integer statusHttp,
    String resumo,
    OffsetDateTime criadoEm
) {
    public static AuditoriaDTO from(Auditoria a) {
        return new AuditoriaDTO(
            a.getId(),
            a.getUsuarioId(),
            a.getUsuarioNome(),
            a.getAcao(),
            a.getModulo(),
            a.getEntidade(),
            a.getEntidadeId(),
            a.getMetodo(),
            a.getCaminho(),
            a.getStatusHttp(),
            a.getResumo(),
            a.getCriadoEm()
        );
    }
}
