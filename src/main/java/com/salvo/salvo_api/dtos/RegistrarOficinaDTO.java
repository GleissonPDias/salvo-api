package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarOficinaDTO(
    @NotBlank(message = "O nome da oficina é obrigatório")
    String nomeOficina,

    @NotBlank(message = "O seu nome é obrigatório")
    String nomeUsuario,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    String password,

    String telefone,
    String cnpj
) {}
