package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AtualizarFornecedorDTO;
import com.salvo.salvo_api.dtos.CriarFornecedorDTO;
import com.salvo.salvo_api.dtos.FornecedorDTO;
import com.salvo.salvo_api.services.FornecedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fornecedores")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro de fornecedores de peças e serviços")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    @GetMapping
    @Operation(summary = "Lista fornecedores da oficina")
    public List<FornecedorDTO> listar(
        @RequestParam(defaultValue = "true") boolean incluirInativos
    ) {
        return fornecedorService.listar(incluirInativos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um fornecedor")
    public FornecedorDTO buscar(@PathVariable Long id) {
        return fornecedorService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um fornecedor")
    public FornecedorDTO criar(@RequestBody @Valid CriarFornecedorDTO dto) {
        return fornecedorService.criar(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um fornecedor")
    public FornecedorDTO atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarFornecedorDTO dto) {
        return fornecedorService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um fornecedor (exclusão lógica)")
    public void desativar(@PathVariable Long id) {
        fornecedorService.desativar(id);
    }
}
