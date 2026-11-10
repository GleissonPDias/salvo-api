package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.ContadorNotificacoesDTO;
import com.salvo.salvo_api.dtos.CriarNotificacaoDTO;
import com.salvo.salvo_api.dtos.NotificacaoDTO;
import com.salvo.salvo_api.services.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Central de notificações da oficina")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    @Operation(summary = "Lista as notificações da oficina")
    public List<NotificacaoDTO> listar(
        @RequestParam(defaultValue = "false") boolean somenteNaoLidas,
        @RequestParam(defaultValue = "100") int limite
    ) {
        return notificacaoService.listar(somenteNaoLidas, limite);
    }

    @GetMapping("/nao-lidas/contador")
    @Operation(summary = "Quantidade de notificações não lidas")
    public ContadorNotificacoesDTO contarNaoLidas() {
        return new ContadorNotificacoesDTO(notificacaoService.contarNaoLidas());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma notificação")
    public NotificacaoDTO criar(@RequestBody @Valid CriarNotificacaoDTO dto) {
        return notificacaoService.criar(dto);
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marca uma notificação como lida")
    public NotificacaoDTO marcarLida(@PathVariable Long id) {
        return notificacaoService.marcarLida(id);
    }

    @PatchMapping("/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marca todas as notificações como lidas")
    public void marcarTodasLidas() {
        notificacaoService.marcarTodasLidas();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma notificação")
    public void excluir(@PathVariable Long id) {
        notificacaoService.excluir(id);
    }
}
