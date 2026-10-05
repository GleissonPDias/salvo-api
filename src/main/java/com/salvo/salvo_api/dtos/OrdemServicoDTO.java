package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.os.PrioridadeOs;
import com.salvo.salvo_api.entities.OrdemServico;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrdemServicoDTO(
    Long id,
    Integer numero,
    String status,
    PrioridadeOs prioridade,
    Long clienteId,
    String clienteNome,
    String clienteTelefone,
    Long veiculoId,
    String veiculoPlaca,
    String veiculoModelo,
    Integer kmEntrada,
    String diagnostico,
    String observacoes,
    BigDecimal desconto,
    BigDecimal totalServicos,
    BigDecimal totalPecas,
    BigDecimal total,
    OffsetDateTime abertaEm,
    OffsetDateTime fechadaEm,
    List<OsServicoDTO> servicos,
    List<OsPecaDTO> pecas
) {
    public static OrdemServicoDTO from(OrdemServico os) {
        BigDecimal totalServicos = os.getServicos().stream()
            .map(s -> s.getQuantidade().multiply(s.getValorUnitario()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPecas = os.getPecas().stream()
            .map(p -> p.getQuantidade().multiply(p.getValorUnitario()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = totalServicos.add(totalPecas).subtract(os.getDesconto());

        return new OrdemServicoDTO(
            os.getId(),
            os.getNumero(),
            os.getStatus().getNome(),
            os.getPrioridade(),
            os.getCliente().getId(),
            os.getCliente().getNome(),
            os.getCliente().getTelefone(),
            os.getVeiculo().getId(),
            os.getVeiculo().getPlaca(),
            os.getVeiculo().getModelo(),
            os.getKmEntrada(),
            os.getDiagnostico(),
            os.getObservacoes(),
            os.getDesconto(),
            totalServicos,
            totalPecas,
            total,
            os.getAbertaEm(),
            os.getFechadaEm(),
            os.getServicos().stream().map(OsServicoDTO::from).toList(),
            os.getPecas().stream().map(OsPecaDTO::from).toList()
        );
    }
}
