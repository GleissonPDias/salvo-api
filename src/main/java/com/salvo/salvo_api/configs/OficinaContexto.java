package com.salvo.salvo_api.configs;

import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.exceptions.AcessoNegadoException;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OficinaContexto {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Usuario getUsuarioLogado() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String sub = jwt.getSubject();

        return usuarioRepository.findByAuthUserId(UUID.fromString(sub))
            .map(usuario -> {
                if (!Boolean.TRUE.equals(usuario.getAtivo())) {
                    throw new AcessoNegadoException(
                        "Usuário inativo. Procure o administrador da oficina."
                    );
                }
                return usuario;
            })
            .orElseThrow(() -> new AcessoNegadoException(
                "Usuário com auth_user_id=" + sub + " não está registrado em nenhuma oficina."
            ));
    }

    @Transactional(readOnly = true)
    public Long getOficinaId() {
        return getUsuarioLogado().getOficina().getId();
    }

    @Transactional(readOnly = true)
    public Long getUsuarioId() {
        return getUsuarioLogado().getId();
    }
}
