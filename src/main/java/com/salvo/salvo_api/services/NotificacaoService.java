package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.CriarNotificacaoDTO;
import com.salvo.salvo_api.dtos.NotificacaoDTO;
import com.salvo.salvo_api.entities.Notificacao;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.repositories.NotificacaoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<NotificacaoDTO> listar(boolean somenteNaoLidas, int limite) {
        Long oficina = oficinaContexto.getOficinaId();
        List<Notificacao> lista = somenteNaoLidas
            ? notificacaoRepository.findByOficinaIdAndLidaFalseOrderByCriadoEmDesc(oficina)
            : notificacaoRepository.findByOficinaIdOrderByCriadoEmDesc(oficina);

        int limit = Math.min(Math.max(limite, 1), 500);
        return lista.stream().limit(limit).map(NotificacaoDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas() {
        return notificacaoRepository.countByOficinaIdAndLidaFalse(oficinaContexto.getOficinaId());
    }

    @Transactional
    public NotificacaoDTO criar(CriarNotificacaoDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();

        Notificacao notificacao = new Notificacao();
        notificacao.setOficina(em.getReference(Oficina.class, oficinaId));
        notificacao.setUsuarioId(dto.usuarioId());
        notificacao.setTipo(limpar(dto.tipo()));
        notificacao.setTitulo(dto.titulo().trim());
        notificacao.setMensagem(limpar(dto.mensagem()));
        notificacao.setLink(limpar(dto.link()));
        notificacao.setLida(false);

        return NotificacaoDTO.from(notificacaoRepository.save(notificacao));
    }

    @Transactional
    public NotificacaoDTO marcarLida(Long id) {
        Notificacao notificacao = exigirNotificacao(id);
        if (!Boolean.TRUE.equals(notificacao.getLida())) {
            notificacao.setLida(true);
            notificacao.setLidaEm(OffsetDateTime.now());
        }
        return NotificacaoDTO.from(notificacaoRepository.save(notificacao));
    }

    @Transactional
    public void marcarTodasLidas() {
        Long oficina = oficinaContexto.getOficinaId();
        List<Notificacao> naoLidas = notificacaoRepository
            .findByOficinaIdAndLidaFalseOrderByCriadoEmDesc(oficina);
        OffsetDateTime agora = OffsetDateTime.now();
        naoLidas.forEach(item -> {
            item.setLida(true);
            item.setLidaEm(agora);
        });
        notificacaoRepository.saveAll(naoLidas);
    }

    @Transactional
    public void excluir(Long id) {
        notificacaoRepository.delete(exigirNotificacao(id));
    }

    private Notificacao exigirNotificacao(Long id) {
        Long oficina = oficinaContexto.getOficinaId();
        return notificacaoRepository.findByIdAndOficinaId(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada: " + id));
    }

    private String limpar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
