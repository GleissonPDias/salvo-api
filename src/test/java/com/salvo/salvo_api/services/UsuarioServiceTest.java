package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.dtos.CriarUsuarioDTO;
import com.salvo.salvo_api.dtos.MecanicoDTO;
import com.salvo.salvo_api.dtos.UsuarioCriadoDTO;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.OficinaRepository;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OficinaRepository oficinaRepository;

    @Mock
    private OficinaContexto oficinaContexto;

    @Mock
    private SupabaseAdminService supabaseAdminService;

    @Mock
    private PermissaoService permissaoService;

    private UsuarioService service;
    private Oficina oficina;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(
            usuarioRepository,
            oficinaRepository,
            oficinaContexto,
            supabaseAdminService,
            permissaoService
        );

        oficina = new Oficina();
        oficina.setId(1L);
        oficina.setNome("Oficina Teste");

        admin = new Usuario();
        admin.setId(10L);
        admin.setOficina(oficina);
        admin.setNome("Admin");
        admin.setEmail("admin@teste.com");
        admin.setCargo("ADMIN");
        admin.setPerfil(PerfilUsuario.ADMIN);
        admin.setAtivo(true);
    }

    @Test
    void criarRejeitaEmailDuplicadoAntesDeChamarSupabase() {
        when(oficinaContexto.getUsuarioLogado()).thenReturn(admin);
        when(usuarioRepository.existsByEmailIgnoreCase("mecanico@teste.com")).thenReturn(true);

        CriarUsuarioDTO dto = new CriarUsuarioDTO(
            "Mecânico", "mecanico@teste.com", "12345678", false,
            "11999999999", "Mecânico", "Motor", false, BigDecimal.ZERO, "#2563eb", true
        );

        assertThatThrownBy(() -> service.criar(dto))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("E-mail já cadastrado");

        verify(supabaseAdminService, never()).criarUsuario(anyString(), anyString(), anyMap());
    }

    @Test
    void criarGeraSenhaTemporariaECriaFuncionario() {
        when(oficinaContexto.getUsuarioLogado()).thenReturn(admin);
        when(usuarioRepository.existsByEmailIgnoreCase("mecanico@teste.com")).thenReturn(false);
        when(oficinaRepository.getReferenceById(1L)).thenReturn(oficina);

        UUID authId = UUID.randomUUID();
        when(supabaseAdminService.criarUsuario(eq("mecanico@teste.com"), anyString(), anyMap()))
            .thenReturn(authId);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario salvo = invocation.getArgument(0);
            salvo.setId(20L);
            salvo.setCriadoEm(OffsetDateTime.now());
            return salvo;
        });

        CriarUsuarioDTO dto = new CriarUsuarioDTO(
            "João Mecânico", "Mecanico@Teste.com", null, false,
            "11999999999", "Mecânico", "Motor e freios", true,
            new BigDecimal("10.00"), "#2563eb", true
        );

        UsuarioCriadoDTO resultado = service.criar(dto);

        assertThat(resultado.senhaTemporaria()).hasSize(12);
        assertThat(resultado.usuario().nome()).isEqualTo("João Mecânico");
        assertThat(resultado.usuario().email()).isEqualTo("mecanico@teste.com");
        assertThat(resultado.usuario().perfil()).isEqualTo(PerfilUsuario.FUNCIONARIO);
        assertThat(resultado.usuario().executaOs()).isTrue();
        assertThat(resultado.usuario().ativo()).isTrue();
        assertThat(resultado.usuario().deveTrocarSenha()).isTrue();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthUserId()).isEqualTo(authId);
        assertThat(captor.getValue().getOficina()).isEqualTo(oficina);
    }

    @Test
    void criarCompensaSupabaseQuandoInsertFalha() {
        when(oficinaContexto.getUsuarioLogado()).thenReturn(admin);
        when(usuarioRepository.existsByEmailIgnoreCase("mecanico@teste.com")).thenReturn(false);
        when(oficinaRepository.getReferenceById(1L)).thenReturn(oficina);

        UUID authId = UUID.randomUUID();
        when(supabaseAdminService.criarUsuario(eq("mecanico@teste.com"), anyString(), anyMap()))
            .thenReturn(authId);
        when(usuarioRepository.save(any(Usuario.class)))
            .thenThrow(new RuntimeException("falha no banco"));

        CriarUsuarioDTO dto = new CriarUsuarioDTO(
            "Mecânico", "mecanico@teste.com", "12345678", false,
            null, "Mecânico", null, false, null, null, true
        );

        assertThatThrownBy(() -> service.criar(dto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("falha no banco");

        verify(supabaseAdminService).excluirUsuario(authId);
    }

    @Test
    void naoPermiteDesativarOProprioUsuario() {
        when(oficinaContexto.getUsuarioLogado()).thenReturn(admin);
        when(usuarioRepository.findByIdAndOficinaId(10L, 1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.alterarStatus(10L, false))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("próprio usuário");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void naoPermiteDesativarUltimoAdminAtivo() {
        Usuario outroAdmin = new Usuario();
        outroAdmin.setId(11L);
        outroAdmin.setOficina(oficina);
        outroAdmin.setPerfil(PerfilUsuario.ADMIN);
        outroAdmin.setAtivo(true);

        when(oficinaContexto.getUsuarioLogado()).thenReturn(admin);
        when(usuarioRepository.findByIdAndOficinaId(11L, 1L)).thenReturn(Optional.of(outroAdmin));
        when(usuarioRepository.countByOficinaIdAndPerfilAndAtivoTrue(1L, PerfilUsuario.ADMIN))
            .thenReturn(1L);

        assertThatThrownBy(() -> service.alterarStatus(11L, false))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("último administrador");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void listaSomenteMecanicosDaOficina() {
        Usuario mecanico = new Usuario();
        mecanico.setId(20L);
        mecanico.setOficina(oficina);
        mecanico.setNome("Mecânico");
        mecanico.setCargo("Mecânico");
        mecanico.setLider(true);
        mecanico.setExecutaOs(true);
        mecanico.setAtivo(true);

        when(oficinaContexto.getOficinaId()).thenReturn(1L);
        when(usuarioRepository.findByOficinaIdAndAtivoTrueAndExecutaOsTrueOrderByLiderDescNomeAsc(1L))
            .thenReturn(List.of(mecanico));

        List<MecanicoDTO> resultado = service.listarMecanicos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nome()).isEqualTo("Mecânico");
        assertThat(resultado.get(0).lider()).isTrue();
    }
}
