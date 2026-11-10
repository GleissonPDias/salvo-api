package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Fornecedor;

import java.time.OffsetDateTime;

public record FornecedorDTO(
    Long id,
    String nome,
    String razaoSocial,
    String cnpj,
    String email,
    String telefone,
    String contato,
    Integer leadTimeDias,
    String endereco,
    String observacoes,
    Boolean ativo,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm
) {
    public static FornecedorDTO from(Fornecedor f) {
        return new FornecedorDTO(
            f.getId(), f.getNome(), f.getRazaoSocial(), f.getCnpj(), f.getEmail(),
            f.getTelefone(), f.getContato(), f.getLeadTimeDias(), f.getEndereco(),
            f.getObservacoes(), f.getAtivo(), f.getCriadoEm(), f.getAtualizadoEm()
        );
    }
}
