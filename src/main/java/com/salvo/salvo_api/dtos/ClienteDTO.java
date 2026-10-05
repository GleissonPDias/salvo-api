package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Cliente;
import java.time.OffsetDateTime;

public record ClienteDTO(
    Long id,
    String nome,
    String cpfCnpj,
    String email,
    String telefone,
    String telefoneSecundario,
    String classificacao,
    Boolean ativo,
    OffsetDateTime criadoEm
) {
    public static ClienteDTO from(Cliente c) {
        return new ClienteDTO(
            c.getId(), c.getNome(), c.getCpfCnpj(),
            c.getEmail(), c.getTelefone(), c.getTelefoneSecundario(),
            c.getClassificacao(), c.getAtivo(), c.getCriadoEm()
        );
    }
}
