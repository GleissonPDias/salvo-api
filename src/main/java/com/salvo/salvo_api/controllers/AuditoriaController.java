package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.dtos.AuditoriaDTO;
import com.salvo.salvo_api.services.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "Relatório das ações feitas no app")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    @PreAuthorize("@permissoes.isAdmin()")
    @Operation(summary = "Lista as ações registradas na oficina (somente administrador)")
    public Page<AuditoriaDTO> listar(
        @RequestParam(required = false) Long usuarioId,
        @RequestParam(required = false) String modulo,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        return auditoriaService.listar(usuarioId, modulo, de, ate, pageable);
    }
}
