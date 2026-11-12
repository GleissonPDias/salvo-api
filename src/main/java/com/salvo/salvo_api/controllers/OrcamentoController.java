package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AtualizarOrcamentoDTO;
import com.salvo.salvo_api.dtos.CriarOrcamentoDTO;
import com.salvo.salvo_api.dtos.OrcamentoDTO;
import com.salvo.salvo_api.services.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
@PreAuthorize("@permissoes.pode('ORCAMENTOS')")
@Tag(name = "Orçamentos", description = "Propostas comerciais")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    @GetMapping
    @Operation(summary = "Lista os orçamentos da oficina")
    public List<OrcamentoDTO> listar() {
        return orcamentoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um orçamento por id")
    public OrcamentoDTO buscar(@PathVariable Long id) {
        return orcamentoService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um orçamento (rascunho)")
    public OrcamentoDTO criar(@RequestBody @Valid CriarOrcamentoDTO dto) {
        return orcamentoService.criar(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um orçamento")
    public OrcamentoDTO atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarOrcamentoDTO dto) {
        return orcamentoService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um orçamento")
    public void excluir(@PathVariable Long id) {
        orcamentoService.excluir(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status (enviado, aprovado, rejeitado...)")
    public OrcamentoDTO alterarStatus(@PathVariable Long id, @RequestParam String status) {
        return orcamentoService.alterarStatus(id, status);
    }

    @PostMapping("/{id}/converter")
    @Operation(summary = "Marca o orçamento como convertido em OS")
    public OrcamentoDTO converter(@PathVariable Long id, @RequestParam Long ordemServicoId) {
        return orcamentoService.converter(id, ordemServicoId);
    }
}
