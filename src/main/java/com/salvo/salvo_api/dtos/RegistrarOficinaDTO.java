package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de uma nova oficina + usuário administrador.
 *
 * O Supabase Auth é a fonte da identidade (e-mail/senha). Depois do signup,
 * o backend cria a oficina e o usuário local vinculados ao auth_user_id.
 */
public record RegistrarOficinaDTO(
    @NotBlank(message = "Nome da oficina é obrigatório") String nomeOficina,
    @NotBlank(message = "Nome do responsável é obrigatório") String nomeUsuario,
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido") String email,
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres") String password,
    String telefone,
    String cnpj
) {}
