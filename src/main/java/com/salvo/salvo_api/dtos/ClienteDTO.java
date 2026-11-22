package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Cliente;
import java.time.OffsetDateTime;

public record ClienteDTO(
    Long id,
    Integer numero,
    String nome,
    String cpfCnpj,
    String email,
    String telefone,
    String telefoneSecundario,
    String classificacao,
    Boolean whatsapp,
    Boolean aceitaComunicacoes,
    String observacoes,
    Boolean ativo,
    OffsetDateTime criadoEm,
    Long ordensConcluidas
) {
    public static ClienteDTO from(Cliente c) {
        return from(c, 0L);
    }

    public static ClienteDTO from(Cliente c, long ordensConcluidas) {
        return new ClienteDTO(
            c.getId(), c.getNumero(), c.getNome(), c.getCpfCnpj(),
            c.getEmail(), c.getTelefone(), c.getTelefoneSecundario(),
            c.getClassificacao(), c.getWhatsapp(), c.getAceitaComunicacoes(),
            c.getObservacoes(), c.getAtivo(), c.getCriadoEm(),
            ordensConcluidas
        );
    }
}
