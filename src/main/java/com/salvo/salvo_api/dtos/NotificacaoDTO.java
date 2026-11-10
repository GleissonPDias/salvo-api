package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Notificacao;

import java.time.OffsetDateTime;

public record NotificacaoDTO(
    Long id,
    Long usuarioId,
    String tipo,
    String titulo,
    String mensagem,
    String link,
    Boolean lida,
    OffsetDateTime criadoEm,
    OffsetDateTime lidaEm
) {
    public static NotificacaoDTO from(Notificacao n) {
        return new NotificacaoDTO(
            n.getId(), n.getUsuarioId(), n.getTipo(), n.getTitulo(),
            n.getMensagem(), n.getLink(), n.getLida(), n.getCriadoEm(), n.getLidaEm()
        );
    }
}
