package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AtualizarVeiculoDTO;
import com.salvo.salvo_api.dtos.CriarVeiculoDTO;
import com.salvo.salvo_api.dtos.VeiculoDTO;
import com.salvo.salvo_api.services.VeiculoService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "Gestão de veículos dos clientes")
public class VeiculoController {

    private final VeiculoService veiculoService;

    @GetMapping("/veiculos")
    @PreAuthorize("@permissoes.podeAlgum('VEICULOS','OS','AGENDA','ORCAMENTOS')")
    @Operation(summary = "Lista todos os veículos da oficina com paginação")
    public Page<VeiculoDTO> listar(@ParameterObject @PageableDefault(size = 20, sort = "modelo") Pageable pageable) {
        return veiculoService.listar(pageable);
    }

    @GetMapping("/veiculos/{id}")
    @PreAuthorize("@permissoes.podeAlgum('VEICULOS','OS','AGENDA','ORCAMENTOS')")
    @Operation(summary = "Busca veículo por ID")
    public VeiculoDTO buscar(@PathVariable Long id) {
        return veiculoService.buscarPorId(id);
    }

    @GetMapping("/clientes/{clienteId}/veiculos")
    @PreAuthorize("@permissoes.podeAlgum('VEICULOS','OS','AGENDA','ORCAMENTOS')")
    @Operation(summary = "Lista todos os veículos de um cliente específico")
    public List<VeiculoDTO> listarPorCliente(@PathVariable Long clienteId) {
        return veiculoService.listarPorCliente(clienteId);
    }

    @PostMapping("/clientes/{clienteId}/veiculos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('VEICULOS')")
    @Operation(summary = "Cadastra um novo veículo para o cliente")
    public VeiculoDTO criar(@PathVariable Long clienteId, @RequestBody @Valid CriarVeiculoDTO dto) {
        return veiculoService.criar(clienteId, dto);
    }

    @PutMapping("/veiculos/{id}")
    @PreAuthorize("@permissoes.pode('VEICULOS')")
    @Operation(summary = "Atualiza os dados de um veículo")
    public VeiculoDTO atualizar(@PathVariable Long id, @RequestBody AtualizarVeiculoDTO dto) {
        return veiculoService.atualizar(id, dto);
    }

    @DeleteMapping("/veiculos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissoes.pode('VEICULOS')")
    @Operation(summary = "Desativa um veículo")
    public void desativar(@PathVariable Long id) {
        veiculoService.desativar(id);
    }
}
