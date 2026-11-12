package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

/**
 * Edição dos dados de um usuário da oficina.
 *
 * O e-mail não é alterável: é a identidade no Supabase Auth. O perfil e as
 * permissões podem ser alterados por um administrador.
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

    Boolean executaOs,

    PerfilUsuario perfil,

    List<ModuloPermissao> permissoes
) {}
