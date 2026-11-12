package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AgendamentoDTO;
import com.salvo.salvo_api.dtos.AlterarStatusAgendamentoDTO;
import com.salvo.salvo_api.dtos.AtualizarAgendamentoDTO;
import com.salvo.salvo_api.dtos.CriarAgendamentoDTO;
import com.salvo.salvo_api.services.AgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
@PreAuthorize("@permissoes.pode('AGENDA')")
@RequiredArgsConstructor
@Tag(name = "Agenda", description = "Agendamentos de serviços da oficina")
public class AgendaController {

    private final AgendaService agendaService;

    @GetMapping
    @Operation(summary = "Lista agendamentos, opcionalmente por status e período")
    public List<AgendamentoDTO> listar(
        @RequestParam(required = false) String status,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim
    ) {
        return agendaService.listar(status, inicio, fim);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um agendamento")
    public AgendamentoDTO buscar(@PathVariable Long id) {
        return agendaService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um agendamento")
    public AgendamentoDTO criar(@RequestBody @Valid CriarAgendamentoDTO dto) {
        return agendaService.criar(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um agendamento")
    public AgendamentoDTO atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarAgendamentoDTO dto) {
        return agendaService.atualizar(id, dto);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status de um agendamento")
    public AgendamentoDTO alterarStatus(
        @PathVariable Long id,
        @RequestBody @Valid AlterarStatusAgendamentoDTO dto
    ) {
        return agendaService.alterarStatus(id, dto.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um agendamento")
    public void excluir(@PathVariable Long id) {
        agendaService.excluir(id);
    }
}
