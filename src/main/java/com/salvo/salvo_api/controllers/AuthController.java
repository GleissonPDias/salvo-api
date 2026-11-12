package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.LoginRequestDTO;
import com.salvo.salvo_api.dtos.LoginResponseDTO;
import com.salvo.salvo_api.dtos.RegistrarOficinaDTO;
import com.salvo.salvo_api.dtos.UsuarioLogadoDTO;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.services.AuthService;
import com.salvo.salvo_api.dtos.AlterarSenhaDTO;
import com.salvo.salvo_api.dtos.TrocarUsuarioDTO;
import com.salvo.salvo_api.dtos.ValidarSenhaDTO;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import com.salvo.salvo_api.services.PermissaoService;
import com.salvo.salvo_api.services.RegistroService;
import com.salvo.salvo_api.services.SupabaseAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e contexto de usuário")
public class AuthController {

    private final OficinaContexto oficinaContexto;
    private final AuthService authService;
    private final RegistroService registroService;
    private final PermissaoService permissaoService;
    private final UsuarioRepository usuarioRepository;
    private final SupabaseAdminService supabaseAdminService;

    @PostMapping("/login")
    @Operation(summary = "Realiza login no Supabase Auth e retorna o access_token (JWT)")
    public LoginResponseDTO login(@RequestBody @Valid LoginRequestDTO request) {
        return authService.login(request);
    }

    @PostMapping("/registrar")
    @Operation(summary = "Cria uma nova oficina e seu usuário administrador")
    public LoginResponseDTO registrar(@RequestBody @Valid RegistrarOficinaDTO request) {
        return registroService.registrar(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna os dados do usuário logado e da sua oficina")
    public UsuarioLogadoDTO me() {
        return UsuarioLogadoDTO.from(
            oficinaContexto.getUsuarioLogado(),
            permissaoService.modulosDoUsuarioLogado()
        );
    }

    @PostMapping("/trocar-usuario")
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Entra como outro usuário da oficina (exige a senha dele)")
    public LoginResponseDTO trocarUsuario(@RequestBody @Valid TrocarUsuarioDTO dto) {
        Usuario admin = oficinaContexto.getUsuarioLogado();
        Usuario alvo = usuarioRepository.findByIdAndOficinaId(dto.usuarioId(), admin.getOficina().getId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + dto.usuarioId()));

        if (!Boolean.TRUE.equals(alvo.getAtivo())) {
            throw new RegraDeNegocioException("Usuário inativo.");
        }
        return authService.autenticar(alvo.getEmail(), dto.senha());
    }

    @PostMapping("/validar-senha")
    @Operation(summary = "Valida a senha de um usuário da oficina (usado para voltar ao admin)")
    public void validarSenha(@RequestBody @Valid ValidarSenhaDTO dto) {
        Usuario atual = oficinaContexto.getUsuarioLogado();
        Usuario alvo = usuarioRepository.findByIdAndOficinaId(dto.usuarioId(), atual.getOficina().getId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + dto.usuarioId()));

        if (!Boolean.TRUE.equals(alvo.getAtivo())) {
            throw new RegraDeNegocioException("Usuário inativo.");
        }
        if (alvo.getPerfil() != PerfilUsuario.ADMIN) {
            throw new RegraDeNegocioException("Somente contas de administrador podem ser usadas para voltar.");
        }
        authService.autenticar(alvo.getEmail(), dto.senha());
    }

    @PostMapping("/alterar-senha")
    @Operation(summary = "Altera a senha do usuário logado e limpa a obrigatoriedade de troca")
    public void alterarSenha(@RequestBody @Valid AlterarSenhaDTO dto) {
        Usuario usuario = oficinaContexto.getUsuarioLogado();
        if (usuario.getAuthUserId() == null) {
            throw new RegraDeNegocioException("Este usuário não possui identidade no Supabase Auth.");
        }
        supabaseAdminService.atualizarSenha(usuario.getAuthUserId(), dto.novaSenha());
        usuario.setDeveTrocarSenha(false);
        usuarioRepository.save(usuario);
    }
}
