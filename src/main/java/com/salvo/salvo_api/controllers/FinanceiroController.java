package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AlterarStatusFinanceiroDTO;
import com.salvo.salvo_api.dtos.AtualizarDespesaDTO;
import com.salvo.salvo_api.dtos.AtualizarPagamentoDTO;
import com.salvo.salvo_api.dtos.CriarDespesaDTO;
import com.salvo.salvo_api.dtos.CriarPagamentoDTO;
import com.salvo.salvo_api.dtos.DespesaDTO;
import com.salvo.salvo_api.dtos.PagamentoDTO;
import com.salvo.salvo_api.services.FinanceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("@permissoes.pode('FINANCEIRO')")
@Tag(name = "Financeiro", description = "Contas a receber (pagamentos) e a pagar (despesas)")
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    /* ------------------------------------------------------------------ */
    /* Pagamentos (contas a receber)                                       */
    /* ------------------------------------------------------------------ */

    @GetMapping("/pagamentos")
    @Operation(summary = "Lista os pagamentos da oficina")
    public List<PagamentoDTO> listarPagamentos(@RequestParam(required = false) String status) {
        return financeiroService.listarPagamentos(status);
    }

    @GetMapping("/pagamentos/{id}")
    @Operation(summary = "Busca um pagamento")
    public PagamentoDTO buscarPagamento(@PathVariable Long id) {
        return financeiroService.buscarPagamento(id);
    }

    @PostMapping("/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um pagamento")
    public PagamentoDTO criarPagamento(@RequestBody @Valid CriarPagamentoDTO dto) {
        return financeiroService.criarPagamento(dto);
    }

    @PutMapping("/pagamentos/{id}")
    @Operation(summary = "Atualiza um pagamento")
    public PagamentoDTO atualizarPagamento(@PathVariable Long id, @RequestBody @Valid AtualizarPagamentoDTO dto) {
        return financeiroService.atualizarPagamento(id, dto);
    }

    @PatchMapping("/pagamentos/{id}/status")
    @Operation(summary = "Altera o status de um pagamento")
    public PagamentoDTO alterarStatusPagamento(
        @PathVariable Long id,
        @RequestBody @Valid AlterarStatusFinanceiroDTO dto
    ) {
        return financeiroService.alterarStatusPagamento(id, dto.status());
    }

    @DeleteMapping("/pagamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um pagamento")
    public void excluirPagamento(@PathVariable Long id) {
        financeiroService.excluirPagamento(id);
    }

    /* ------------------------------------------------------------------ */
    /* Despesas (contas a pagar)                                           */
    /* ------------------------------------------------------------------ */

    @GetMapping("/despesas")
    @Operation(summary = "Lista as despesas da oficina")
    public List<DespesaDTO> listarDespesas(@RequestParam(required = false) String status) {
        return financeiroService.listarDespesas(status);
    }

    @GetMapping("/despesas/{id}")
    @Operation(summary = "Busca uma despesa")
    public DespesaDTO buscarDespesa(@PathVariable Long id) {
        return financeiroService.buscarDespesa(id);
    }

    @PostMapping("/despesas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma despesa")
    public DespesaDTO criarDespesa(@RequestBody @Valid CriarDespesaDTO dto) {
        return financeiroService.criarDespesa(dto);
    }

    @PutMapping("/despesas/{id}")
    @Operation(summary = "Atualiza uma despesa")
    public DespesaDTO atualizarDespesa(@PathVariable Long id, @RequestBody @Valid AtualizarDespesaDTO dto) {
        return financeiroService.atualizarDespesa(id, dto);
    }

    @PatchMapping("/despesas/{id}/status")
    @Operation(summary = "Altera o status de uma despesa")
    public DespesaDTO alterarStatusDespesa(
        @PathVariable Long id,
        @RequestBody @Valid AlterarStatusFinanceiroDTO dto
    ) {
        return financeiroService.alterarStatusDespesa(id, dto.status());
    }

    @DeleteMapping("/despesas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma despesa")
    public void excluirDespesa(@PathVariable Long id) {
        financeiroService.excluirDespesa(id);
    }
}
