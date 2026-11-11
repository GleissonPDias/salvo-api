package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AlterarStatusUsuarioDTO;
import com.salvo.salvo_api.dtos.AtualizarUsuarioDTO;
import com.salvo.salvo_api.dtos.CriarUsuarioDTO;
import com.salvo.salvo_api.dtos.RedefinirSenhaResponseDTO;
import com.salvo.salvo_api.dtos.UsuarioCriadoDTO;
import com.salvo.salvo_api.dtos.UsuarioDTO;
import com.salvo.salvo_api.exceptions.AcessoNegadoException;
import com.salvo.salvo_api.services.PermissaoService;
import com.salvo.salvo_api.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Equipe", description = "Cadastro e gestão de funcionários da oficina")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PermissaoService permissaoService;

    @GetMapping
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Lista a equipe da oficina (somente administrador)")
    public List<UsuarioDTO> listar(
        @RequestParam(required = false) String busca,
        @RequestParam(required = false) Boolean ativo,
        @RequestParam(required = false) Boolean executaOs
    ) {
        return usuarioService.listar(busca, ativo, executaOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário (administrador ou o próprio usuário)")
    public UsuarioDTO buscar(@PathVariable Long id) {
        if (!permissaoService.isAdmin() && !permissaoService.isSelf(id)) {
            throw new AcessoNegadoException("Você só pode consultar o seu próprio usuário");
        }
        return usuarioService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Cadastra um funcionário e cria a identidade no Supabase Auth")
    public UsuarioCriadoDTO criar(@RequestBody @Valid CriarUsuarioDTO dto) {
        return usuarioService.criar(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Atualiza os dados operacionais de um funcionário")
    public UsuarioDTO atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarUsuarioDTO dto) {
        return usuarioService.atualizar(id, dto);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Ativa ou desativa um funcionário")
    public UsuarioDTO alterarStatus(
        @PathVariable Long id,
        @RequestBody @Valid AlterarStatusUsuarioDTO dto
    ) {
        return usuarioService.alterarStatus(id, dto.ativo());
    }

    @PostMapping("/{id}/reset-senha")
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Gera uma senha temporária para o funcionário")
    public RedefinirSenhaResponseDTO redefinirSenha(@PathVariable Long id) {
        return usuarioService.redefinirSenha(id);
    }
}
