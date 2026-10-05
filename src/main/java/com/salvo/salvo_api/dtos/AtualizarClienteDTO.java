package com.salvo.salvo_api.dtos;

public record AtualizarClienteDTO (
      String nome,
      String email,
      String telefone,
      String telefoneSecundario,
      String classificacao
){}
