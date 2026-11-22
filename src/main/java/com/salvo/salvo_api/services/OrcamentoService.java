package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarOrcamentoDTO;
import com.salvo.salvo_api.dtos.CriarOrcamentoDTO;
import com.salvo.salvo_api.dtos.CriarOrcamentoItemDTO;
import com.salvo.salvo_api.dtos.OrcamentoDTO;
import com.salvo.salvo_api.dtos.OrcamentoItemDTO;
import com.salvo.salvo_api.entities.Orcamento;
import com.salvo.salvo_api.entities.Cliente;
import com.salvo.salvo_api.entities.OrcamentoItem;
import com.salvo.salvo_api.entities.Veiculo;
import com.salvo.salvo_api.entities.OrdemServico;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.repositories.ClienteRepository;
import com.salvo.salvo_api.repositories.OrcamentoRepository;
import com.salvo.salvo_api.repositories.VeiculoRepository;
import com.salvo.salvo_api.repositories.OrdemServicoRepository;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private static final Set<String> STATUS_VALIDOS =
        Set.of("RASCUNHO", "ENVIADO", "APROVADO", "REJEITADO", "CONVERTIDO", "CANCELADO");

    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<OrcamentoDTO> listar() {
        Long oficinaId = oficinaContexto.getOficinaId();
        return orcamentoRepository.findByOficinaIdOrderByNumeroDesc(oficinaId).stream()
            .map(o -> OrcamentoDTO.from(o, nomeCriador(o.getCriadoPor()), nomeCriador(o.getVendedorId()), nomeCliente(o.getClienteId()), placaVeiculo(o.getVeiculoId()), modeloVeiculo(o.getVeiculoId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoDTO buscar(Long id) {
        Orcamento o = exigir(id);
        return OrcamentoDTO.from(o, nomeCriador(o.getCriadoPor()), nomeCriador(o.getVendedorId()));
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
        orcamento.setVendedorId(dto.vendedorId());
        orcamento.setTitulo(dto.titulo());
        orcamento.setTipo(dto.tipo());
        orcamento.setObservacoes(dto.observacoes());
        orcamento.setValidade(dto.validade());
        orcamento.setValidadeDias(dto.validadeDias() != null ? dto.validadeDias() : (short) 7);
        orcamento.setDesconto(dto.desconto() != null ? dto.desconto() : BigDecimal.ZERO);
        orcamento.setDescontoTipo(dto.descontoTipo() != null ? dto.descontoTipo().trim().toUpperCase() : "VALOR");
        orcamento.setCanalEnvio(dto.canalEnvio());
        orcamento.setStatus("RASCUNHO");

        if (dto.itens() != null) {
            for (CriarOrcamentoItemDTO item : dto.itens()) {
                orcamento.getItens().add(toItem(orcamento, item));
            }
        }

        Orcamento salvo = orcamentoRepository.save(orcamento);
        return OrcamentoDTO.from(salvo, oficinaContexto.getUsuarioLogado().getNome(), nomeCriador(salvo.getVendedorId()), nomeCliente(salvo.getClienteId()), placaVeiculo(salvo.getVeiculoId()), modeloVeiculo(salvo.getVeiculoId()));
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
        if (dto.status() != null) orcamento.setStatus(normalizarStatus(dto.status()));

        if (dto.itens() != null) {
            orcamento.getItens().clear();
            for (CriarOrcamentoItemDTO item : dto.itens()) {
                orcamento.getItens().add(toItem(orcamento, item));
            }
        }

        Orcamento salvo = orcamentoRepository.save(orcamento);
        return OrcamentoDTO.from(salvo, nomeCriador(salvo.getCriadoPor()), nomeCriador(salvo.getVendedorId()), nomeCliente(salvo.getClienteId()), placaVeiculo(salvo.getVeiculoId()), modeloVeiculo(salvo.getVeiculoId()));
    }

    @Transactional
    public void excluir(Long id) {
        orcamentoRepository.delete(exigir(id));
    }

    @Transactional
    public OrcamentoDTO alterarStatus(Long id, String status, String motivo, String canal) {
        Orcamento orcamento = exigir(id);
        String novo = normalizarStatus(status);
        orcamento.setStatus(novo);
        OffsetDateTime now = OffsetDateTime.now();
        if ("ENVIADO".equals(novo)) {
            if (orcamento.getEmitidoEm() == null) orcamento.setEmitidoEm(now);
            if (canal != null && !canal.isBlank()) orcamento.setCanalEnvio(canal.trim());
        }
        if ("APROVADO".equals(novo)) orcamento.setAprovadoEm(now);
        if ("REJEITADO".equals(novo)) {
            orcamento.setRejeitadoEm(now);
            if (motivo != null && !motivo.isBlank()) orcamento.setMotivoRejeicao(motivo.trim());
            if (canal != null && !canal.isBlank()) orcamento.setCanalEnvio(canal.trim());
        }
        Orcamento salvo = orcamentoRepository.save(orcamento);
        return OrcamentoDTO.from(salvo, nomeCriador(salvo.getCriadoPor()), nomeCriador(salvo.getVendedorId()));
    }

    @Transactional
    public OrcamentoDTO converter(Long id, Long ordemServicoIdOuNumero) {
        Orcamento orcamento = exigir(id);
        Long oficinaId = oficinaContexto.getOficinaId();

        OrdemServico ordem = ordemServicoRepository.findById(ordemServicoIdOuNumero).orElse(null);
        if (ordem == null || !ordem.getOficina().getId().equals(oficinaId)) {
            ordem = ordemServicoRepository.findByOficinaIdAndNumero(oficinaId, ordemServicoIdOuNumero.intValue()).orElse(null);
        }
        if (ordem == null) {
            throw new RecursoNaoEncontradoException("OS não encontrada: " + ordemServicoIdOuNumero);
        }

        orcamento.setOrdemServicoId(ordem.getId());
        orcamento.setStatus("CONVERTIDO");
        Orcamento salvo = orcamentoRepository.save(orcamento);
        return OrcamentoDTO.from(salvo, nomeCriador(salvo.getCriadoPor()), nomeCriador(salvo.getVendedorId()));
    }

    private String normalizarStatus(String status) {
        String valor = status == null ? "" : status.trim().toUpperCase();
        if (!STATUS_VALIDOS.contains(valor)) {
            throw new RegraDeNegocioException("Status de orçamento inválido: " + status);
        }
        return valor;
    }

    private Orcamento exigir(Long id) {
        Long oficinaId = oficinaContexto.getOficinaId();
        return orcamentoRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento não encontrado: " + id));
    }

    private OrcamentoItem toItem(Orcamento orcamento, CriarOrcamentoItemDTO dto) {
        OrcamentoItem item = new OrcamentoItem();
        item.setOrcamento(orcamento);
        item.setTipo(dto.tipo().trim().toUpperCase());
        item.setPecaId(dto.pecaId());
        item.setServicoId(dto.servicoId());
        item.setCodigo(dto.codigo());
        item.setDescricao(dto.descricao().trim());
        item.setQuantidade(dto.quantidade());
        item.setValorUnitario(dto.valorUnitario());
        item.setTempoEstimadoHoras(dto.tempoEstimadoHoras());
        return item;
    }

    private String nomeCliente(Long clienteId) {
        if (clienteId == null) return null;
        return clienteRepository.findById(clienteId).map(Cliente::getNome).orElse(null);
    }

    private String placaVeiculo(Long veiculoId) {
        if (veiculoId == null) return null;
        return veiculoRepository.findById(veiculoId).map(Veiculo::getPlaca).orElse(null);
    }

    private String modeloVeiculo(Long veiculoId) {
        if (veiculoId == null) return null;
        return veiculoRepository.findById(veiculoId).map(Veiculo::getModelo).orElse(null);
    }

    private String nomeCriador(Long usuarioId) {
        if (usuarioId == null) return null;
        return usuarioRepository.findById(usuarioId).map(Usuario::getNome).orElse(null);
    }
}
