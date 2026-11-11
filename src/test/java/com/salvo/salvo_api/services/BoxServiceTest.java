package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarBoxDTO;
import com.salvo.salvo_api.dtos.CriarBoxDTO;
import com.salvo.salvo_api.entities.Box;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.BoxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoxServiceTest {

    @Mock
    private BoxRepository boxRepository;

    @Mock
    private OficinaContexto oficinaContexto;

    private BoxService service;

    @BeforeEach
    void setUp() {
        service = new BoxService(boxRepository, oficinaContexto);
        when(oficinaContexto.getOficinaId()).thenReturn(1L);
    }

    @Test
    void criarRejeitaNomeDuplicadoIgnorandoCaixaEEspacos() {
        Box existente = box(7L, "Box 01");
        when(boxRepository.findByOficinaIdOrderByNomeAsc(1L)).thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.criar(new CriarBoxDTO("  box 01 ", null)))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("Já existe um box com este nome");
    }

    @Test
    void atualizarPermiteManteroProprioNome() {
        Box existente = box(7L, "Box 01");
        when(boxRepository.findByIdAndOficinaId(7L, 1L)).thenReturn(Optional.of(existente));
        when(boxRepository.findByOficinaIdOrderByNomeAsc(1L)).thenReturn(List.of(existente));
        when(boxRepository.save(existente)).thenReturn(existente);

        service.atualizar(7L, new AtualizarBoxDTO("Box 01", "Elevador", null));

        org.assertj.core.api.Assertions.assertThat(existente.getDescricao()).isEqualTo("Elevador");
    }

    @Test
    void atualizarRejeitaNomeEmBranco() {
        when(boxRepository.findByIdAndOficinaId(7L, 1L)).thenReturn(Optional.of(box(7L, "Box 01")));

        assertThatThrownBy(() -> service.atualizar(7L, new AtualizarBoxDTO("   ", null, null)))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("não pode ficar em branco");
    }

    private Box box(Long id, String nome) {
        Box box = new Box();
        box.setId(id);
        box.setNome(nome);
        box.setAtivo(true);
        return box;
    }
}
