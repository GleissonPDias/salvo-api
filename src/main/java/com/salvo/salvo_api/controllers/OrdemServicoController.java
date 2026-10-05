package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.*;
import com.salvo.salvo_api.services.OrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Ciclo de vida e gestão de Ordens de Serviço")
public class OrdemServicoController {

    private final OrdemServicoService osService;

    @GetMapping
    @Operation(summary = "Lista ordens de serviço com filtros e paginação")
    public Page<OrdemServicoResumoDTO> listar(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long clienteId,
        @ParameterObject @PageableDefault(size = 20, sort = "numero") Pageable pageable
    ) {
        return osService.listar(status, clienteId, pageable);
    }

    @GetMapping("/{numero}")
    @Operation(summary = "Busca detalhes completos de uma OS pelo número")
    public OrdemServicoDTO buscar(@PathVariable Integer numero) {
        return osService.buscarPorNumero(numero);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre uma nova Ordem de Serviço")
    public OrdemServicoDTO criar(@RequestBody @Valid CriarOsDTO dto) {
        return osService.criar(dto);
    }

    @PatchMapping("/{numero}/status")
    @Operation(summary = "Avança ou altera o status de uma OS")
    public OrdemServicoDTO alterarStatus(
        @PathVariable Integer numero,
        @RequestBody @Valid AlterarStatusOsDTO dto
    ) {
        return osService.alterarStatus(numero, dto);
    }

    @GetMapping("/{numero}/historico")
    @Operation(summary = "Lista o histórico de alterações de status da OS")
    public List<OsHistoricoDTO> historico(@PathVariable Integer numero) {
        return osService.listarHistorico(numero);
    }

    @PostMapping("/{numero}/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona um serviço à OS")
    public OsServicoDTO adicionarServico(
        @PathVariable Integer numero,
        @RequestBody @Valid AdicionarServicoOsDTO dto
    ) {
        return osService.adicionarServico(numero, dto);
    }

    @DeleteMapping("/{numero}/servicos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um serviço da OS")
    public void removerServico(@PathVariable Integer numero, @PathVariable Long id) {
        osService.removerServico(numero, id);
    }

    @PostMapping("/{numero}/pecas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona uma peça à OS")
    public OsPecaDTO adicionarPeca(
        @PathVariable Integer numero,
        @RequestBody @Valid AdicionarPecaOsDTO dto
    ) {
        return osService.adicionarPeca(numero, dto);
    }

    @DeleteMapping("/{numero}/pecas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma peça da OS")
    public void removerPeca(@PathVariable Integer numero, @PathVariable Long id) {
        osService.removerPeca(numero, id);
    }
}
