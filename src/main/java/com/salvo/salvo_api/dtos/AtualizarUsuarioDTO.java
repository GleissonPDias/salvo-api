package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Edição dos dados operacionais de um funcionário.
 *
 * E-mail e perfil não são alteráveis neste endpoint na v1:
 * o e-mail é a identidade no Supabase Auth e o perfil é fixo em FUNCIONARIO.
 */
public record AtualizarUsuarioDTO(
    String nome,
    String telefone,
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
