package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "servico_pecas_sugeridas")
@IdClass(ServicoPecaSugeridaId.class)
@Getter
@Setter
public class ServicoPecaSugerida {

    @Id
    @Column(name = "servico_id", nullable = false)
    private Long servicoId;

    @Id
    @Column(name = "peca_id", nullable = false)
    private Long pecaId;
}
