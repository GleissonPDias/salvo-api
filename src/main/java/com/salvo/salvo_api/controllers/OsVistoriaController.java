package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.OsVistoriaDTO;
import com.salvo.salvo_api.dtos.RegistrarVistoriaDTO;
import com.salvo.salvo_api.services.OsVistoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ordens-servico")
@PreAuthorize("@permissoes.pode('OS')")
@RequiredArgsConstructor
@Tag(name = "Vistoria de entrada", description = "Vistoria e avarias da OS")
public class OsVistoriaController {

    private final OsVistoriaService vistoriaService;

    @GetMapping("/{numero}/vistoria")
    @Operation(summary = "Busca a vistoria da OS (vazia se ainda não registrada)")
    public OsVistoriaDTO buscar(@PathVariable Integer numero) {
        return vistoriaService.buscarPorNumero(numero);
    }

    @PutMapping("/{numero}/vistoria")
    @Operation(summary = "Cria ou substitui a vistoria da OS")
    public OsVistoriaDTO salvar(
        @PathVariable Integer numero,
        @RequestBody @Valid RegistrarVistoriaDTO dto
    ) {
        return vistoriaService.salvar(numero, dto);
    }
}
