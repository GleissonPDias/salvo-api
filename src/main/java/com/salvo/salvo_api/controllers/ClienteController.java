package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AtualizarClienteDTO;
import com.salvo.salvo_api.dtos.ClienteDTO;
import com.salvo.salvo_api.dtos.CriarClienteDTO;
import com.salvo.salvo_api.services.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestão de clientes da oficina")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @PreAuthorize("@permissoes.podeAlgum('CLIENTES','OS','AGENDA','ORCAMENTOS')")
    @Operation(summary = "Lista clientes com paginação e busca por nome/cpf")
    public Page<ClienteDTO> listar(
        @RequestParam(required = false) String q,
        @ParameterObject @PageableDefault(size = 20, sort = "nome") Pageable pageable
    ) {
        return clienteService.listar(q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.podeAlgum('CLIENTES','OS','AGENDA','ORCAMENTOS')")
    @Operation(summary = "Busca cliente por ID")
    public ClienteDTO buscar(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('CLIENTES')")
    @Operation(summary = "Cadastra um novo cliente")
    public ClienteDTO criar(@RequestBody @Valid CriarClienteDTO dto) {
        return clienteService.criar(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissoes.pode('CLIENTES')")
    @Operation(summary = "Atualiza os dados de um cliente")
    public ClienteDTO atualizar(@PathVariable Long id, @RequestBody AtualizarClienteDTO dto) {
        return clienteService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissoes.pode('CLIENTES')")
    @Operation(summary = "Desativa um cliente")
    public void desativar(@PathVariable Long id) {
        clienteService.desativar(id);
    }
}
