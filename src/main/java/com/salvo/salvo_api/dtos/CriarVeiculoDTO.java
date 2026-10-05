package com.salvo.salvo_api.dtos;
import jakarta.validation.constraints.NotBlank;
public record CriarVeiculoDTO(

    @NotBlank(message = "Placa é obrigatória") String placa,
    String chassi,
    @NotBlank(message = "Marca é obrigatória") String marca,
    @NotBlank(message = "Modelo é obrigatório") String modelo,
    String versao,
    Short anoFabricacao,
    Short anoModelo,
    String cor,
    String combustivel,
    String motorizacao,
    String transmissao,
    Integer kmAtual
) {}
