package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AtualizarBoxDTO;
import com.salvo.salvo_api.dtos.BoxDTO;
import com.salvo.salvo_api.dtos.CriarBoxDTO;
import com.salvo.salvo_api.services.BoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boxes")
@RequiredArgsConstructor
@Tag(name = "Boxes", description = "Boxes de atendimento da oficina")
public class BoxController {

    private final BoxService boxService;

    @GetMapping
    @Operation(summary = "Lista os boxes da oficina (ativos, salvo incluirInativos=true)")
    public List<BoxDTO> listar(@RequestParam(defaultValue = "false") boolean incluirInativos) {
        return boxService.listar(incluirInativos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Cria um box (somente administrador)")
    public BoxDTO criar(@RequestBody @Valid CriarBoxDTO dto) {
        return boxService.criar(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Atualiza um box (somente administrador)")
    public BoxDTO atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarBoxDTO dto) {
        return boxService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Desativa um box (somente administrador)")
    public void desativar(@PathVariable Long id) {
        boxService.desativar(id);
    }
}
