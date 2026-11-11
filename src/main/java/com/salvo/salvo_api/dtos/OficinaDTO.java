package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Oficina;

import java.math.BigDecimal;

/** Dados cadastrais e de configuração da oficina (cabeçalho de impressão, orçamentos e OS). */
public record OficinaDTO(
    Long id,
    String nome,
    String nomeFantasia,
    String cnpj,
    String telefone,
    String whatsapp,
    String email,
    String site,
    String logoUrl,
    String cep,
    String logradouro,
    String numero,
    String complemento,
    String bairro,
    String cidade,
    String uf,
    String horarioFuncionamento,
    Integer validadeOrcamentoDias,
    BigDecimal descontoMaximoPercentual,
    String corPrimaria,
    Boolean ativa
) {
    public static OficinaDTO from(Oficina o) {
        return new OficinaDTO(
            o.getId(), o.getNome(), o.getNomeFantasia(), o.getCnpj(), o.getTelefone(),
            o.getWhatsapp(), o.getEmail(), o.getSite(), o.getLogoUrl(), o.getCep(),
            o.getLogradouro(), o.getNumero(), o.getComplemento(), o.getBairro(), o.getCidade(),
            o.getUf(), o.getHorarioFuncionamento(), o.getValidadeOrcamentoDias(),
            o.getDescontoMaximoPercentual(), o.getCorPrimaria(), o.getAtiva()
        );
    }
}
