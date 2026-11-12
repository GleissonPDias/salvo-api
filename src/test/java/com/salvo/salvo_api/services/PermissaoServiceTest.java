package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.repositories.UsuarioPermissaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissaoServiceTest {

    @Mock
    private OficinaContexto oficinaContexto;

    @Mock
    private UsuarioPermissaoRepository usuarioPermissaoRepository;

    private PermissaoService service;

    @BeforeEach
    void setUp() {
        service = new PermissaoService(oficinaContexto, usuarioPermissaoRepository);
    }

    @Test
    void adminPodeQualquerModuloSemConsultarPermissoes() {
        Usuario admin = usuario(1L, PerfilUsuario.ADMIN);
        when(oficinaContexto.getUsuarioLogado()).thenReturn(admin);

        assertThat(service.pode("CLIENTES")).isTrue();
        assertThat(service.pode("FINANCEIRO")).isTrue();
        assertThat(service.podeAlgum("OS", "AGENDA")).isTrue();
        assertThat(service.modulosDoUsuarioLogado())
            .containsExactlyInAnyOrder(ModuloPermissao.values());
    }

    @Test
    void funcionarioSoPodeOsModulosLiberados() {
        Usuario funcionario = usuario(2L, PerfilUsuario.FUNCIONARIO);
        when(oficinaContexto.getUsuarioLogado()).thenReturn(funcionario);
        when(usuarioPermissaoRepository.buscarModulos(2L))
            .thenReturn(List.of(ModuloPermissao.OS, ModuloPermissao.AGENDA));

        assertThat(service.pode("OS")).isTrue();
        assertThat(service.pode("AGENDA")).isTrue();
        assertThat(service.pode("FINANCEIRO")).isFalse();
        assertThat(service.podeAlgum("CLIENTES", "FINANCEIRO")).isFalse();
        assertThat(service.podeAlgum("CLIENTES", "OS")).isTrue();
    }

    @Test
    void moduloInvalidoNaoLiberaAcesso() {
        assertThat(service.pode("MODULO_QUE_NAO_EXISTE")).isFalse();
        assertThat(service.pode(null)).isFalse();
    }

    private Usuario usuario(Long id, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }
}
