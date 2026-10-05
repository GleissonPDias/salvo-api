package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.LoginRequestDTO;
import com.salvo.salvo_api.dtos.LoginResponseDTO;
import com.salvo.salvo_api.dtos.UsuarioLogadoDTO;
import com.salvo.salvo_api.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e contexto de usuário")
public class AuthController {

    private final OficinaContexto oficinaContexto;
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Realiza login no Supabase Auth e retorna o access_token (JWT)")
    public LoginResponseDTO login(@RequestBody @Valid LoginRequestDTO request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna os dados do usuário logado e da sua oficina")
    public UsuarioLogadoDTO me() {
        return UsuarioLogadoDTO.from(oficinaContexto.getUsuarioLogado());
    }
}
