package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Usuario;

/**
 * Payload enxuto para o dropdown de mecânico responsável.
 * Não expõe e-mail, telefone ou comissão para todos os usuários da oficina.
 */
public record MecanicoDTO(
    Long id,
    String nome,
    String cargo,
    String especialidades,
    Boolean lider,
    String corAgenda,
    String fotoUrl
) {
    public static MecanicoDTO from(Usuario u) {
        return new MecanicoDTO(
            u.getId(),
            u.getNome(),
            u.getCargo(),
            u.getEspecialidades(),
            u.getLider(),
            u.getCorAgenda(),
            u.getFotoUrl()
        );
    }
}
