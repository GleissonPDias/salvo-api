package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cadastro de um funcionário pelo administrador da oficina.
 *
 * O perfil é sempre FUNCIONARIO na v1. O campo executaOs define se ele
 * aparece como mecânico responsável na OS.
 */
public record CriarUsuarioDTO(
    @NotBlank(message = "Nome é obrigatório")
    String nome,

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @Size(min = 8, max = 72, message = "A senha inicial deve ter entre 8 e 72 caracteres")
    String senhaInicial,

    Boolean enviarConvite,

    String telefone,

    @NotBlank(message = "Cargo é obrigatório")
    String cargo,

    String especialidades,

    Boolean lider,

    @DecimalMin(value = "0.00", message = "Comissão não pode ser negativa")
    @DecimalMax(value = "100.00", message = "Comissão não pode passar de 100%")
    BigDecimal comissaoPercentual,

    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "Cor da agenda deve estar no formato #RRGGBB")
    String corAgenda,

    Boolean executaOs
) {}
