package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.os.PrioridadeOs;
import com.salvo.salvo_api.entities.OrdemServico;

import java.time.OffsetDateTime;

public record OrdemServicoResumoDTO(
    Long id,
    Integer numero,
    String status,
    PrioridadeOs prioridade,
    String clienteNome,
    String veiculoPlaca,
    String veiculoModelo,
    Long boxId,
    Long mecanicoLiderId,
    String tipo,
    String canal,
    String titulo,
    OffsetDateTime previsaoEntrega,
    OffsetDateTime abertaEm,
    OffsetDateTime prontaEm,
    OffsetDateTime entregueEm,
    OffsetDateTime canceladaEm,
    OffsetDateTime fechadaEm
) {
    public static OrdemServicoResumoDTO from(OrdemServico os) {
        return new OrdemServicoResumoDTO(
            os.getId(),
            os.getNumero(),
            os.getStatus().getNome(),
            os.getPrioridade(),
            os.getCliente().getNome(),
            os.getVeiculo().getPlaca(),
            os.getVeiculo().getModelo(),
            os.getBox() != null ? os.getBox().getId() : null,
            os.getMecanicoLider() != null ? os.getMecanicoLider().getId() : null,
            os.getTipo(),
            os.getCanal(),
            os.getTitulo(),
            os.getPrevisaoEntrega(),
            os.getAbertaEm(),
            os.getProntaEm(),
            os.getEntregueEm(),
            os.getCanceladaEm(),
            os.getFechadaEm()
        );
    }
}
