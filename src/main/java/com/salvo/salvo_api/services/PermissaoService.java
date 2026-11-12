package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.exceptions.AcessoNegadoException;
import com.salvo.salvo_api.repositories.UsuarioPermissaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Regras de autorização baseadas no perfil e nas permissões do usuário logado.
 *
 * O JWT do Supabase não carrega o perfil da oficina; por isso a decisão é
 * sempre feita no banco, a partir do auth_user_id do token.
 *
 * Administradores têm acesso a todos os módulos. Funcionários só acessam os
 * módulos marcados em usuario_permissoes. A checagem no back-end é a fonte
 * da verdade; o front apenas esconde menus e rotas.
 */
@Service("permissoes")
@RequiredArgsConstructor
public class PermissaoService {

    private final OficinaContexto oficinaContexto;
    private final UsuarioPermissaoRepository usuarioPermissaoRepository;

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

    /**
     * Usado nos @PreAuthorize dos controllers. O módulo é passado como texto
     * para manter as anotações legíveis: @PreAuthorize("@permissoes.pode('OS')").
     */
    public boolean pode(String modulo) {
        ModuloPermissao alvo = parse(modulo);
        if (alvo == null) return false;

        Usuario usuario = getUsuarioLogado();
        if (usuario.getPerfil() == PerfilUsuario.ADMIN) return true;
        return usuarioPermissaoRepository.buscarModulos(usuario.getId()).contains(alvo);
    }

    /**
     * Libera o acesso quando o usuário tem pelo menos um dos módulos.
     * Usado em leituras compartilhadas, como a busca de clientes e veículos
     * usada pelos formulários de OS, agenda e orçamento.
     */
    public boolean podeAlgum(String... modulos) {
        if (modulos == null || modulos.length == 0) return false;

        Usuario usuario = getUsuarioLogado();
        if (usuario.getPerfil() == PerfilUsuario.ADMIN) return true;

        List<ModuloPermissao> alvos = Arrays.stream(modulos)
            .map(this::parse)
            .filter(java.util.Objects::nonNull)
            .toList();
        if (alvos.isEmpty()) return false;

        return usuarioPermissaoRepository.buscarModulos(usuario.getId()).stream().anyMatch(alvos::contains);
    }

    public void exigirAdmin() {
        if (!isAdmin()) {
            throw new AcessoNegadoException("Apenas o administrador da oficina pode executar esta ação.");
        }
    }

    public void exigirModulo(String modulo) {
        if (!pode(modulo)) {
            throw new AcessoNegadoException("Você não tem permissão para acessar este módulo.");
        }
    }

    /** Módulos liberados para o usuário logado. Admin recebe todos. */
    public List<ModuloPermissao> modulosDoUsuarioLogado() {
        return modulosDoUsuario(getUsuarioLogado());
    }

    /** Módulos liberados para um usuário. Admin recebe todos. */
    public List<ModuloPermissao> modulosDoUsuario(Usuario usuario) {
        if (usuario == null) return List.of();
        if (usuario.getPerfil() == PerfilUsuario.ADMIN) {
            return List.of(ModuloPermissao.values());
        }
        return usuarioPermissaoRepository.buscarModulos(usuario.getId());
    }

    private Usuario getUsuarioLogado() {
        return oficinaContexto.getUsuarioLogado();
    }

    private ModuloPermissao parse(String modulo) {
        if (modulo == null || modulo.isBlank()) return null;
        try {
            return ModuloPermissao.valueOf(modulo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
