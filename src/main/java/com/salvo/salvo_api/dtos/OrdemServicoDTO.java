package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.os.PrioridadeOs;
import com.salvo.salvo_api.entities.OrdemServico;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    Long boxId,
    String boxNome,
    Long mecanicoLiderId,
    String mecanicoLiderNome,
    Long criadoPorId,
    String criadoPorNome,
    String tipo,
    String canal,
    String titulo,
    String relatoCliente,
    String observacoes,
    String observacoesInternas,
    String observacoesCliente,
    Integer kmEntrada,
    Integer kmSaida,
    Short nivelCombustivel,
    OffsetDateTime previsaoEntrega,
    String diagnostico,
    Boolean diagnosticoValidado,
    BigDecimal desconto,
    String descontoTipo,
    String formaPagamento,
    Short parcelas,
    String garantiaTipo,
    Integer garantiaDias,
    Integer garantiaKm,
    String aprovacaoStatus,
    String aprovacaoCanal,
    OffsetDateTime aprovacaoEnviadaEm,
    OffsetDateTime aprovacaoDecididaEm,
    OffsetDateTime aprovacaoVisualizadaEm,
    BigDecimal totalServicos,
    BigDecimal totalPecas,
    BigDecimal total,
    OffsetDateTime abertaEm,
    OffsetDateTime prontaEm,
    OffsetDateTime entregueEm,
    OffsetDateTime canceladaEm,
    OffsetDateTime fechadaEm,
    String motivoCancelamento,
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

        BigDecimal subtotal = totalServicos.add(totalPecas);
        BigDecimal desconto = os.getDesconto() != null ? os.getDesconto() : BigDecimal.ZERO;
        BigDecimal descontoAplicado = "PERCENTUAL".equalsIgnoreCase(os.getDescontoTipo())
            ? subtotal.multiply(desconto).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : desconto;

        if (descontoAplicado.compareTo(subtotal) > 0) {
            descontoAplicado = subtotal;
        }

        BigDecimal total = subtotal.subtract(descontoAplicado);

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
            os.getBox() != null ? os.getBox().getId() : null,
            os.getBox() != null ? os.getBox().getNome() : null,
            os.getMecanicoLider() != null ? os.getMecanicoLider().getId() : null,
            os.getMecanicoLider() != null ? os.getMecanicoLider().getNome() : null,
            os.getUsuarioAbertura() != null ? os.getUsuarioAbertura().getId() : null,
            os.getUsuarioAbertura() != null ? os.getUsuarioAbertura().getNome() : null,
            os.getTipo(),
            os.getCanal(),
            os.getTitulo(),
            os.getRelatoCliente(),
            os.getObservacoes(),
            os.getObservacoesInternas(),
            os.getObservacoesCliente(),
            os.getKmEntrada(),
            os.getKmSaida(),
            os.getNivelCombustivel(),
            os.getPrevisaoEntrega(),
            os.getDiagnostico(),
            os.getDiagnosticoValidado(),
            os.getDesconto(),
            os.getDescontoTipo(),
            os.getFormaPagamento(),
            os.getParcelas(),
            os.getGarantiaTipo(),
            os.getGarantiaDias(),
            os.getGarantiaKm(),
            os.getAprovacaoStatus(),
            os.getAprovacaoCanal(),
            os.getAprovacaoEnviadaEm(),
            os.getAprovacaoDecididaEm(),
            os.getAprovacaoVisualizadaEm(),
            totalServicos,
            totalPecas,
            total,
            os.getAbertaEm(),
            os.getProntaEm(),
            os.getEntregueEm(),
            os.getCanceladaEm(),
            os.getFechadaEm(),
            os.getMotivoCancelamento(),
            os.getServicos().stream().map(OsServicoDTO::from).toList(),
            os.getPecas().stream().map(OsPecaDTO::from).toList()
        );
    }
}
