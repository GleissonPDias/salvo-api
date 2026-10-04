package com.salvo.salvo_api.entities;

import java.time.OffsetDateTime;
import java.util.List;

public record ErroDTO(
    String codigo,
    String mensagem,
    List<String> detalhes,
    OffsetDateTime timestamp
) {
    public ErroDTO(String codigo, String mensagem){
        this(codigo, mensagem, List.of(), OffsetDateTime.now());
    }

    public ErroDTO(String codigo, String mensagem, List<String> detalhes){
        this(codigo, mensagem, detalhes, OffsetDateTime.now());
    }
}
