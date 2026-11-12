package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import com.salvo.salvo_api.dtos.AuditoriaDTO;
import com.salvo.salvo_api.entities.Auditoria;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final OficinaContexto oficinaContexto;

    /**
     * Grava a ação em transação própria: a auditoria não pode ser desfeita
     * junto com a operação de negócio e não deve derrubar a requisição.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(
        Usuario usuario,
        String acao,
        String modulo,
        String entidade,
        String entidadeId,
        String metodo,
        String caminho,
        Integer statusHttp,
        String resumo
    ) {
        Auditoria auditoria = new Auditoria();
        auditoria.setOficinaId(usuario.getOficina().getId());
        auditoria.setUsuarioId(usuario.getId());
        auditoria.setUsuarioNome(usuario.getNome());
        auditoria.setAcao(acao);
        auditoria.setModulo(modulo);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setMetodo(metodo);
        auditoria.setCaminho(caminho);
        auditoria.setStatusHttp(statusHttp);
        auditoria.setResumo(resumo);
        auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaDTO> listar(
        Long usuarioId,
        String modulo,
        LocalDate de,
        LocalDate ate,
        Pageable pageable
    ) {
        Long oficinaId = oficinaContexto.getOficinaId();
        OffsetDateTime inicio = inicioDoDia(de);
        OffsetDateTime fim = fimDoDia(ate);
        String moduloNormalizado = normalizarModulo(modulo);

        Specification<Auditoria> filtro = (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(root.get("oficinaId"), oficinaId));
            if (usuarioId != null) {
                condicoes.add(cb.equal(root.get("usuarioId"), usuarioId));
            }
            if (moduloNormalizado != null) {
                condicoes.add(cb.equal(root.get("modulo"), moduloNormalizado));
            }
            if (inicio != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), inicio));
            }
            if (fim != null) {
                condicoes.add(cb.lessThanOrEqualTo(root.get("criadoEm"), fim));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };

        return auditoriaRepository.findAll(filtro, pageable).map(AuditoriaDTO::from);
    }

    private String normalizarModulo(String modulo) {
        if (modulo == null || modulo.isBlank()) return null;
        try {
            return ModuloPermissao.valueOf(modulo.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new RegraDeNegocioException("Módulo inválido para o filtro: " + modulo);
        }
    }

    private OffsetDateTime inicioDoDia(LocalDate data) {
        return data == null ? null : data.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private OffsetDateTime fimDoDia(LocalDate data) {
        return data == null
            ? null
            : data.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime().minusNanos(1);
    }
}
