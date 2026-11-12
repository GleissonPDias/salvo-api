package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AlterarStatusPedidoCompraDTO;
import com.salvo.salvo_api.dtos.AtualizarPedidoCompraDTO;
import com.salvo.salvo_api.dtos.CriarPedidoCompraDTO;
import com.salvo.salvo_api.dtos.PedidoCompraDTO;
import com.salvo.salvo_api.services.PedidoCompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos-compra")
@PreAuthorize("@permissoes.pode('ESTOQUE')")
@RequiredArgsConstructor
@Tag(name = "Pedidos de compra", description = "Reposição de estoque via fornecedores")
public class PedidoCompraController {

    private final PedidoCompraService pedidoCompraService;

    @GetMapping
    @Operation(summary = "Lista pedidos de compra da oficina")
    public List<PedidoCompraDTO> listar(@RequestParam(required = false) String status) {
        return pedidoCompraService.listar(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um pedido de compra")
    public PedidoCompraDTO buscar(@PathVariable Long id) {
        return pedidoCompraService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um pedido de compra")
    public PedidoCompraDTO criar(@RequestBody @Valid CriarPedidoCompraDTO dto) {
        return pedidoCompraService.criar(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um pedido de compra")
    public PedidoCompraDTO atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarPedidoCompraDTO dto) {
        return pedidoCompraService.atualizar(id, dto);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status do pedido; ao receber, dá entrada no estoque")
    public PedidoCompraDTO alterarStatus(
        @PathVariable Long id,
        @RequestBody @Valid AlterarStatusPedidoCompraDTO dto
    ) {
        return pedidoCompraService.alterarStatus(id, dto.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um pedido em rascunho ou cancelado")
    public void excluir(@PathVariable Long id) {
        pedidoCompraService.excluir(id);
    }
}
