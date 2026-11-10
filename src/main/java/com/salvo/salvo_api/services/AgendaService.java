package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AgendamentoDTO;
import com.salvo.salvo_api.dtos.AtualizarAgendamentoDTO;
import com.salvo.salvo_api.dtos.CriarAgendamentoDTO;
import com.salvo.salvo_api.entities.Agendamento;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.AgendamentoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgendaService {

    private static final Set<String> STATUS_VALIDOS =
        Set.of("AGENDADO", "CONFIRMADO", "EM_ANDAMENTO", "CONCLUIDO", "CANCELADO", "NAO_COMPARECEU");

    private final AgendamentoRepository agendamentoRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<AgendamentoDTO> listar(String status, OffsetDateTime inicio, OffsetDateTime fim) {
        Long oficina = oficinaContexto.getOficinaId();
        List<Agendamento> lista = status == null || status.isBlank()
            ? agendamentoRepository.findByOficinaIdOrderByInicioAsc(oficina)
            : agendamentoRepository.findByOficinaIdAndStatusOrderByInicioAsc(
                oficina, normalizar(status));

        return lista.stream()
            .filter(item -> inicio == null || !item.getInicio().isBefore(inicio))
            .filter(item -> fim == null || item.getInicio().isBefore(fim))
            .map(AgendamentoDTO::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoDTO buscar(Long id) {
        return AgendamentoDTO.from(exigirAgendamento(id));
    }

    @Transactional
    public AgendamentoDTO criar(CriarAgendamentoDTO dto) {
        validarPeriodo(dto.inicio(), dto.fim());

        Long oficinaId = oficinaContexto.getOficinaId();
        Long usuarioId = oficinaContexto.getUsuarioId();

        Agendamento agendamento = new Agendamento();
        agendamento.setOficina(em.getReference(Oficina.class, oficinaId));
        agendamento.setClienteId(dto.clienteId());
        agendamento.setVeiculoId(dto.veiculoId());
        agendamento.setServicoId(dto.servicoId());
        agendamento.setMecanicoId(dto.mecanicoId());
        agendamento.setBoxId(dto.boxId());
        agendamento.setOrdemServicoId(dto.ordemServicoId());
        agendamento.setTitulo(dto.titulo().trim());
        agendamento.setDescricao(limpar(dto.descricao()));
        agendamento.setInicio(dto.inicio());
        agendamento.setFim(dto.fim());
        agendamento.setStatus(dto.status() != null ? normalizar(dto.status()) : "AGENDADO");
        agendamento.setCriadoPor(usuarioId);

        return AgendamentoDTO.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoDTO atualizar(Long id, AtualizarAgendamentoDTO dto) {
        Agendamento agendamento = exigirAgendamento(id);

        if (dto.clienteId() != null) agendamento.setClienteId(dto.clienteId());
        if (dto.veiculoId() != null) agendamento.setVeiculoId(dto.veiculoId());
        if (dto.servicoId() != null) agendamento.setServicoId(dto.servicoId());
        if (dto.mecanicoId() != null) agendamento.setMecanicoId(dto.mecanicoId());
        if (dto.boxId() != null) agendamento.setBoxId(dto.boxId());
        if (dto.ordemServicoId() != null) agendamento.setOrdemServicoId(dto.ordemServicoId());
        if (dto.titulo() != null && !dto.titulo().isBlank()) agendamento.setTitulo(dto.titulo().trim());
        if (dto.descricao() != null) agendamento.setDescricao(limpar(dto.descricao()));
        if (dto.inicio() != null) agendamento.setInicio(dto.inicio());
        if (dto.fim() != null) agendamento.setFim(dto.fim());
        if (dto.status() != null) agendamento.setStatus(normalizar(dto.status()));

        validarPeriodo(agendamento.getInicio(), agendamento.getFim());

        return AgendamentoDTO.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoDTO alterarStatus(Long id, String status) {
        Agendamento agendamento = exigirAgendamento(id);
        agendamento.setStatus(normalizar(status));
        return AgendamentoDTO.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public void excluir(Long id) {
        agendamentoRepository.delete(exigirAgendamento(id));
    }

    private void validarPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
        if (inicio == null || fim == null) {
            throw new RegraDeNegocioException("Início e fim do agendamento são obrigatórios.");
        }
        if (!fim.isAfter(inicio)) {
            throw new RegraDeNegocioException("O fim do agendamento deve ser depois do início.");
        }
    }

    private Agendamento exigirAgendamento(Long id) {
        Long oficina = oficinaContexto.getOficinaId();
        return agendamentoRepository.findByIdAndOficinaId(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + id));
    }

    private String normalizar(String status) {
        String valor = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_VALIDOS.contains(valor)) {
            throw new RegraDeNegocioException(
                "Status de agendamento inválido: " + status + ". Use " + String.join(", ", STATUS_VALIDOS) + "."
            );
        }
        return valor;
    }

    private String limpar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
