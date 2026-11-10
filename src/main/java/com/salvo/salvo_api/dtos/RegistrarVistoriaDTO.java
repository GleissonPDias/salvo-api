package com.salvo.salvo_api.dtos;

import jakarta.validation.Valid;

import java.time.OffsetDateTime;
import java.util.List;

public record RegistrarVistoriaDTO(
    Boolean clienteCiente,
    String pertences,
    String observacoes,
    OffsetDateTime realizadaEm,
    List<@Valid VistoriaDanoDTO> danos,
    List<@Valid VistoriaFotoDTO> fotos
) {}
