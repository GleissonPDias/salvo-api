package com.salvo.salvo_api.domain.usuario;

/**
 * Módulos operacionais que podem ser liberados por usuário.
 *
 * Equipe, Configurações da oficina e a gestão de usuários não entram
 * nesta lista: continuam exclusivas de administradores.
 */
public enum ModuloPermissao {
    CLIENTES("Clientes"),
    VEICULOS("Veículos"),
    OS("Ordens de serviço"),
    ORCAMENTOS("Orçamentos"),
    ESTOQUE("Estoque"),
    AGENDA("Agenda"),
    FINANCEIRO("Financeiro");

    private final String descricao;

    ModuloPermissao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
