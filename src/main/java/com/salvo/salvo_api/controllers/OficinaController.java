package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AtualizarOficinaDTO;
import com.salvo.salvo_api.dtos.OficinaDTO;
import com.salvo.salvo_api.services.OficinaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oficina")
@RequiredArgsConstructor
@Tag(name = "Oficina", description = "Dados cadastrais e configurações da oficina")
public class OficinaController {

    private final OficinaService oficinaService;

    @GetMapping
    @Operation(summary = "Dados da oficina do usuário logado")
    public OficinaDTO buscar() {
        return oficinaService.buscar();
    }

    @PutMapping
    @Operation(summary = "Atualiza os dados da oficina (somente administrador)")
    public OficinaDTO atualizar(@RequestBody @Valid AtualizarOficinaDTO dto) {
        return oficinaService.atualizar(dto);
    }
}
