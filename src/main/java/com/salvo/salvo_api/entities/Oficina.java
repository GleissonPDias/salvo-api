package com.salvo.salvo_api.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name="oficinas")
@Getter
@Setter
public class Oficina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String cnpj;

    private String telefone;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    private String email;

    private String whatsapp;

    private String site;

    @Column(name = "logo_url", columnDefinition = "text")
    private String logoUrl;

    private String cep;

    private String logradouro;

    private String numero;

    private String complemento;

    private String bairro;

    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(name = "horario_funcionamento")
    private String horarioFuncionamento;

    @Column(name = "validade_orcamento_dias", nullable = false)
    private Integer validadeOrcamentoDias = 7;

    @Column(name = "desconto_maximo_percentual", nullable = false)
    private java.math.BigDecimal descontoMaximoPercentual = java.math.BigDecimal.ZERO;

    @Column(name = "cor_primaria", length = 7)
    private String corPrimaria;

    @Column(nullable = false)
    private Boolean ativa = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name="atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    public void prePersist(){
        criadoEm = OffsetDateTime.now();
        atualizadoEm = OffsetDateTime.now();
    }
}

