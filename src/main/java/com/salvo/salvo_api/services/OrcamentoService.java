package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarOrcamentoDTO;
import com.salvo.salvo_api.dtos.CriarOrcamentoDTO;
import com.salvo.salvo_api.dtos.CriarOrcamentoItemDTO;
import com.salvo.salvo_api.dtos.OrcamentoDTO;
import com.salvo.salvo_api.dtos.OrcamentoItemDTO;
import com.salvo.salvo_api.entities.Orcamento;
import com.salvo.salvo_api.domain.orcamento.StatusOrcamento;
import com.salvo.salvo_api.domain.orcamento.TipoOrcamentoItem;
import com.salvo.salvo_api.entities.OrcamentoItem;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.repositories.OrcamentoRepository;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<OrcamentoDTO> listar() {
        Long oficinaId = oficinaContexto.getOficinaId();
        return orcamentoRepository.findByOficinaIdOrderByNumeroDesc(oficinaId).stream()
            .map(o -> OrcamentoDTO.from(o, nomeCriador(o.getCriadoPor())))
            .toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoDTO buscar(Long id) {
        Orcamento o = exigir(id);
        return OrcamentoDTO.from(o, nomeCriador(o.getCriadoPor()));
    }

    @Transactional
    public OrcamentoDTO criar(CriarOrcamentoDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Long usuarioId = oficinaContexto.getUsuarioId();

        Orcamento orcamento = new Orcamento();
        orcamento.setOficina(em.getReference(Oficina.class, oficinaId));
        orcamento.setNumero(orcamentoRepository.maxNumero(oficinaId) + 1);
        orcamento.setClienteId(dto.clienteId());
        orcamento.setVeiculoId(dto.veiculoId());
        orcamento.setCriadoPor(usuarioId);
        orcamento.setTitulo(dto.titulo());
        orcamento.setTipo(dto.tipo());
        orcamento.setObservacoes(dto.observacoes());
        orcamento.setValidade(dto.validade());
        orcamento.setValidadeDias(dto.validadeDias() != null ? dto.validadeDias() : (short) 7);
        orcamento.setDesconto(dto.desconto() != null ? dto.desconto() : BigDecimal.ZERO);
        orcamento.setDescontoTipo(dto.descontoTipo() != null ? dto.descontoTipo().trim().toUpperCase() : "VALOR");
        orcamento.setCanalEnvio(dto.canalEnvio());
        orcamento.setStatus(StatusOrcamento.RASCUNHO);

        if (dto.itens() != null) {
            for (CriarOrcamentoItemDTO item : dto.itens()) {
                orcamento.getItens().add(toItem(orcamento, item));
            }
        }

        Orcamento salvo = orcamentoRepository.save(orcamento);
        return OrcamentoDTO.from(salvo, oficinaContexto.getUsuarioLogado().getNome());
    }

    @Transactional
    public OrcamentoDTO atualizar(Long id, AtualizarOrcamentoDTO dto) {
        Orcamento orcamento = exigir(id);

        if (dto.clienteId() != null) orcamento.setClienteId(dto.clienteId());
        if (dto.veiculoId() != null) orcamento.setVeiculoId(dto.veiculoId());
        if (dto.titulo() != null) orcamento.setTitulo(dto.titulo());
        if (dto.tipo() != null) orcamento.setTipo(dto.tipo());
        if (dto.observacoes() != null) orcamento.setObservacoes(dto.observacoes());
        if (dto.validade() != null) orcamento.setValidade(dto.validade());
        if (dto.validadeDias() != null) orcamento.setValidadeDias(dto.validadeDias());
        if (dto.desconto() != null) orcamento.setDesconto(dto.desconto());
        if (dto.descontoTipo() != null) orcamento.setDescontoTipo(dto.descontoTipo().trim().toUpperCase());
        if (dto.canalEnvio() != null) orcamento.setCanalEnvio(dto.canalEnvio());
        if (dto.status() != null) orcamento.setStatus(StatusOrcamento.valueOf(dto.status().trim().toUpperCase()));

        if (dto.itens() != null) {
            orcamento.getItens().clear();
            for (CriarOrcamentoItemDTO item : dto.itens()) {
                orcamento.getItens().add(toItem(orcamento, item));
            }
        }

        Orcamento salvo = orcamentoRepository.save(orcamento);
        return OrcamentoDTO.from(salvo, nomeCriador(salvo.getCriadoPor()));
    }

    @Transactional
    public void excluir(Long id) {
        orcamentoRepository.delete(exigir(id));
    }

    @Transactional
    public OrcamentoDTO alterarStatus(Long id, String status) {
        Orcamento orcamento = exigir(id);
        StatusOrcamento novo = StatusOrcamento.valueOf(status.trim().toUpperCase());
        orcamento.setStatus(novo);
        OffsetDateTime now = OffsetDateTime.now();
        if (novo == StatusOrcamento.ENVIADO && orcamento.getEmitidoEm() == null) orcamento.setEmitidoEm(now);
        if (novo == StatusOrcamento.APROVADO) orcamento.setAprovadoEm(now);
        if (novo == StatusOrcamento.REJEITADO) orcamento.setRejeitadoEm(now);
        Orcamento salvo = orcamentoRepository.save(orcamento);
        return OrcamentoDTO.from(salvo, nomeCriador(salvo.getCriadoPor()));
    }

    @Transactional
    public OrcamentoDTO converter(Long id, Long ordemServicoId) {
        Orcamento orcamento = exigir(id);
        orcamento.setOrdemServicoId(ordemServicoId);
        orcamento.setStatus(StatusOrcamento.CONVERTIDO);
        Orcamento salvo = orcamentoRepository.save(orcamento);
        return OrcamentoDTO.from(salvo, nomeCriador(salvo.getCriadoPor()));
    }

    private Orcamento exigir(Long id) {
        Long oficinaId = oficinaContexto.getOficinaId();
        return orcamentoRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento não encontrado: " + id));
    }

    private OrcamentoItem toItem(Orcamento orcamento, CriarOrcamentoItemDTO dto) {
        OrcamentoItem item = new OrcamentoItem();
        item.setOrcamento(orcamento);
        item.setTipo(TipoOrcamentoItem.valueOf(dto.tipo().trim().toUpperCase()));
        item.setPecaId(dto.pecaId());
        item.setServicoId(dto.servicoId());
        item.setCodigo(dto.codigo());
        item.setDescricao(dto.descricao().trim());
        item.setQuantidade(dto.quantidade());
        item.setValorUnitario(dto.valorUnitario());
        item.setTempoEstimadoHoras(dto.tempoEstimadoHoras());
        return item;
    }

    private String nomeCriador(Long usuarioId) {
        if (usuarioId == null) return null;
        return usuarioRepository.findById(usuarioId).map(Usuario::getNome).orElse(null);
    }
}
