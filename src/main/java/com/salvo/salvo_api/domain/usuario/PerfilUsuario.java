package com.salvo.salvo_api.domain.usuario;

/**
 * Perfil de acesso do usuário.
 *
 * ADMIN: dono da oficina, acesso total.
 * FUNCIONARIO: acesso operacional, sem Financeiro, Equipe, Configurações
 * e relatórios financeiros.
 *
 * A função exibida (Mecânico, Atendente etc.) continua no campo "cargo".
 * A possibilidade de ser escolhido como mecânico responsável fica em
 * "executa_os", separada do perfil de acesso.
 */
public enum PerfilUsuario {
    ADMIN,
    FUNCIONARIO
}
