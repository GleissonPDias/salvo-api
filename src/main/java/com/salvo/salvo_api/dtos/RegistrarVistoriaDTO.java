package com.salvo.salvo_api.dtos;

import jakarta.validation.Valid;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record RegistrarVistoriaDTO(
    Boolean clienteCiente,
    String pertences,
    String observacoes,
    OffsetDateTime realizadaEm,
    List<@Valid VistoriaDanoDTO> danos,
    Map<String, String> checklist
) {}
