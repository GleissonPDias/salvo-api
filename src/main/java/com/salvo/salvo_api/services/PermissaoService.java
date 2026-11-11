package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.exceptions.AcessoNegadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras de autorização baseadas no perfil do usuário logado.
 *
 * O JWT do Supabase não carrega o perfil da oficina; por isso a decisão é
 * sempre feita no banco, a partir do auth_user_id do token.
 */
@Service("permissoes")
@RequiredArgsConstructor
public class PermissaoService {

    private final OficinaContexto oficinaContexto;

    public boolean isAdmin() {
        return getUsuarioLogado().getPerfil() == PerfilUsuario.ADMIN;
    }

    public boolean isFuncionario() {
        return getUsuarioLogado().getPerfil() == PerfilUsuario.FUNCIONARIO;
    }

    public boolean isSelf(Long id) {
        if (id == null) return false;
        return id.equals(oficinaContexto.getUsuarioId());
    }

    public void exigirAdmin() {
        if (!isAdmin()) {
            throw new AcessoNegadoException("Apenas o administrador da oficina pode executar esta ação.");
        }
    }

    private Usuario getUsuarioLogado() {
        return oficinaContexto.getUsuarioLogado();
    }
}
