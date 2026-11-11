package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Atualização dos dados da oficina. Campos nulos não são alterados; campos vazios limpam o valor. */
public record AtualizarOficinaDTO(
    @NotBlank(message = "Nome da oficina é obrigatório")
    @Size(max = 120)
    String nome,

    @Size(max = 120) String nomeFantasia,
    @Size(max = 18) String cnpj,
    @Size(max = 20) String telefone,
    @Size(max = 20) String whatsapp,
    @Size(max = 120) String email,
    @Size(max = 160) String site,
    String logoUrl,
    @Size(max = 9) String cep,
    @Size(max = 160) String logradouro,
    @Size(max = 20) String numero,
    @Size(max = 80) String complemento,
    @Size(max = 80) String bairro,
    @Size(max = 80) String cidade,

    @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "UF deve ter 2 letras")
    String uf,

    @Size(max = 160) String horarioFuncionamento,

    @Min(value = 1, message = "Validade do orçamento deve ser de pelo menos 1 dia")
    @Max(value = 365, message = "Validade do orçamento não pode passar de 365 dias")
    Integer validadeOrcamentoDias,

    @DecimalMin(value = "0.00") @DecimalMax(value = "100.00")
    BigDecimal descontoMaximoPercentual,

    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "Cor primária deve estar no formato #RRGGBB")
    String corPrimaria
) {}
