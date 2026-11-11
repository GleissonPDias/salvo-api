package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarBoxDTO;
import com.salvo.salvo_api.dtos.BoxDTO;
import com.salvo.salvo_api.dtos.CriarBoxDTO;
import com.salvo.salvo_api.entities.Box;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.BoxRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoxService {

    private final BoxRepository boxRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<BoxDTO> listar(boolean incluirInativos) {
        Long oficinaId = oficinaContexto.getOficinaId();
        return boxRepository.findByOficinaIdOrderByNomeAsc(oficinaId).stream()
            .filter(box -> incluirInativos || Boolean.TRUE.equals(box.getAtivo()))
            .map(BoxDTO::from)
            .toList();
    }

    @Transactional
    public BoxDTO criar(CriarBoxDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        verificarNomeLivre(dto.nome(), oficinaId, null);

        Box box = new Box();
        box.setOficina(em.getReference(Oficina.class, oficinaId));
        box.setNome(dto.nome().trim());
        box.setDescricao(limpar(dto.descricao()));
        box.setAtivo(true);
        return BoxDTO.from(boxRepository.save(box));
    }

    @Transactional
    public BoxDTO atualizar(Long id, AtualizarBoxDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Box box = exigirBox(id, oficinaId);

        if (dto.nome() != null) {
            if (dto.nome().isBlank()) throw new RegraDeNegocioException("Nome do box não pode ficar em branco");
            verificarNomeLivre(dto.nome(), oficinaId, id);
            box.setNome(dto.nome().trim());
        }
        if (dto.descricao() != null) box.setDescricao(limpar(dto.descricao()));
        if (dto.ativo() != null) box.setAtivo(dto.ativo());

        return BoxDTO.from(boxRepository.save(box));
    }

    /** Desativa o box: OS e agendamentos antigos continuam referenciando-o. */
    @Transactional
    public void desativar(Long id) {
        Box box = exigirBox(id, oficinaContexto.getOficinaId());
        box.setAtivo(false);
        boxRepository.save(box);
    }

    private Box exigirBox(Long id, Long oficinaId) {
        return boxRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Box não encontrado: " + id));
    }

    private void verificarNomeLivre(String nome, Long oficinaId, Long ignorarId) {
        String normalizado = nome.trim();
        boolean duplicado = boxRepository.findByOficinaIdOrderByNomeAsc(oficinaId).stream()
            .anyMatch(box -> box.getNome().equalsIgnoreCase(normalizado)
                && (ignorarId == null || !box.getId().equals(ignorarId)));
        if (duplicado) throw new RegraDeNegocioException("Já existe um box com este nome");
    }

    private String limpar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
