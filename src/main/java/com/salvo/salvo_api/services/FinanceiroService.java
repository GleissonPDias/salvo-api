package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarDespesaDTO;
import com.salvo.salvo_api.dtos.AtualizarPagamentoDTO;
import com.salvo.salvo_api.dtos.CriarDespesaDTO;
import com.salvo.salvo_api.dtos.CriarPagamentoDTO;
import com.salvo.salvo_api.dtos.DespesaDTO;
import com.salvo.salvo_api.dtos.PagamentoDTO;
import com.salvo.salvo_api.entities.Despesa;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.entities.Pagamento;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.DespesaRepository;
import com.salvo.salvo_api.repositories.PagamentoRepository;
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
public class FinanceiroService {

    private static final Set<String> STATUS_PAGAMENTO =
        Set.of("PENDENTE", "PAGO", "CANCELADO", "ESTORNADO");
    private static final Set<String> STATUS_DESPESA =
        Set.of("PENDENTE", "PAGO", "CANCELADO");

    private final PagamentoRepository pagamentoRepository;
    private final DespesaRepository despesaRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    /* ------------------------------------------------------------------ */
    /* Pagamentos                                                          */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public List<PagamentoDTO> listarPagamentos(String status) {
        Long oficina = oficinaContexto.getOficinaId();
        List<Pagamento> lista = status == null || status.isBlank()
            ? pagamentoRepository.findByOficinaIdOrderByCriadoEmDesc(oficina)
            : pagamentoRepository.findByOficinaIdAndStatusOrderByCriadoEmDesc(
                oficina, normalizar(status, STATUS_PAGAMENTO, "pagamento"));
        return lista.stream().map(PagamentoDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public PagamentoDTO buscarPagamento(Long id) {
        return PagamentoDTO.from(exigirPagamento(id));
    }

    @Transactional
    public PagamentoDTO criarPagamento(CriarPagamentoDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Long usuarioId = oficinaContexto.getUsuarioId();

        Pagamento pagamento = new Pagamento();
        pagamento.setOficina(em.getReference(Oficina.class, oficinaId));
        pagamento.setOrdemServicoId(dto.ordemServicoId());
        pagamento.setOrcamentoId(dto.orcamentoId());
        pagamento.setClienteId(dto.clienteId());
        pagamento.setDescricao(limpar(dto.descricao()));
        pagamento.setValor(dto.valor());
        pagamento.setFormaPagamento(limpar(dto.formaPagamento()));
        pagamento.setParcelas(dto.parcelas() != null ? dto.parcelas() : 1);
        pagamento.setStatus(dto.status() != null
            ? normalizar(dto.status(), STATUS_PAGAMENTO, "pagamento")
            : "PENDENTE");
        pagamento.setVencimento(dto.vencimento());
        pagamento.setPagoEm(dto.pagoEm());
        pagamento.setObservacoes(limpar(dto.observacoes()));
        pagamento.setCriadoPor(usuarioId);
        aplicarPagamento(pagamento);

        return PagamentoDTO.from(pagamentoRepository.save(pagamento));
    }

    @Transactional
    public PagamentoDTO atualizarPagamento(Long id, AtualizarPagamentoDTO dto) {
        Pagamento pagamento = exigirPagamento(id);

        if (dto.ordemServicoId() != null) pagamento.setOrdemServicoId(dto.ordemServicoId());
        if (dto.orcamentoId() != null) pagamento.setOrcamentoId(dto.orcamentoId());
        if (dto.clienteId() != null) pagamento.setClienteId(dto.clienteId());
        if (dto.descricao() != null) pagamento.setDescricao(limpar(dto.descricao()));
        if (dto.valor() != null) pagamento.setValor(dto.valor());
        if (dto.formaPagamento() != null) pagamento.setFormaPagamento(limpar(dto.formaPagamento()));
        if (dto.parcelas() != null) pagamento.setParcelas(dto.parcelas());
        if (dto.status() != null) pagamento.setStatus(normalizar(dto.status(), STATUS_PAGAMENTO, "pagamento"));
        if (dto.vencimento() != null) pagamento.setVencimento(dto.vencimento());
        if (dto.pagoEm() != null) pagamento.setPagoEm(dto.pagoEm());
        if (dto.observacoes() != null) pagamento.setObservacoes(limpar(dto.observacoes()));
        aplicarPagamento(pagamento);

        return PagamentoDTO.from(pagamentoRepository.save(pagamento));
    }

    @Transactional
    public PagamentoDTO alterarStatusPagamento(Long id, String status) {
        Pagamento pagamento = exigirPagamento(id);
        pagamento.setStatus(normalizar(status, STATUS_PAGAMENTO, "pagamento"));
        aplicarPagamento(pagamento);
        return PagamentoDTO.from(pagamentoRepository.save(pagamento));
    }

    @Transactional
    public void excluirPagamento(Long id) {
        pagamentoRepository.delete(exigirPagamento(id));
    }

    /* ------------------------------------------------------------------ */
    /* Despesas                                                            */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public List<DespesaDTO> listarDespesas(String status) {
        Long oficina = oficinaContexto.getOficinaId();
        List<Despesa> lista = status == null || status.isBlank()
            ? despesaRepository.findByOficinaIdOrderByCriadoEmDesc(oficina)
            : despesaRepository.findByOficinaIdAndStatusOrderByCriadoEmDesc(
                oficina, normalizar(status, STATUS_DESPESA, "despesa"));
        return lista.stream().map(DespesaDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public DespesaDTO buscarDespesa(Long id) {
        return DespesaDTO.from(exigirDespesa(id));
    }

    @Transactional
    public DespesaDTO criarDespesa(CriarDespesaDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Long usuarioId = oficinaContexto.getUsuarioId();

        Despesa despesa = new Despesa();
        despesa.setOficina(em.getReference(Oficina.class, oficinaId));
        despesa.setFornecedorId(dto.fornecedorId());
        despesa.setDescricao(dto.descricao().trim());
        despesa.setCategoria(limpar(dto.categoria()));
        despesa.setValor(dto.valor());
        despesa.setStatus(dto.status() != null
            ? normalizar(dto.status(), STATUS_DESPESA, "despesa")
            : "PENDENTE");
        despesa.setVencimento(dto.vencimento());
        despesa.setPagoEm(dto.pagoEm());
        despesa.setObservacoes(limpar(dto.observacoes()));
        despesa.setCriadoPor(usuarioId);
        aplicarDespesa(despesa);

        return DespesaDTO.from(despesaRepository.save(despesa));
    }

    @Transactional
    public DespesaDTO atualizarDespesa(Long id, AtualizarDespesaDTO dto) {
        Despesa despesa = exigirDespesa(id);

        if (dto.fornecedorId() != null) despesa.setFornecedorId(dto.fornecedorId());
        if (dto.descricao() != null && !dto.descricao().isBlank()) despesa.setDescricao(dto.descricao().trim());
        if (dto.categoria() != null) despesa.setCategoria(limpar(dto.categoria()));
        if (dto.valor() != null) despesa.setValor(dto.valor());
        if (dto.status() != null) despesa.setStatus(normalizar(dto.status(), STATUS_DESPESA, "despesa"));
        if (dto.vencimento() != null) despesa.setVencimento(dto.vencimento());
        if (dto.pagoEm() != null) despesa.setPagoEm(dto.pagoEm());
        if (dto.observacoes() != null) despesa.setObservacoes(limpar(dto.observacoes()));
        aplicarDespesa(despesa);

        return DespesaDTO.from(despesaRepository.save(despesa));
    }

    @Transactional
    public DespesaDTO alterarStatusDespesa(Long id, String status) {
        Despesa despesa = exigirDespesa(id);
        despesa.setStatus(normalizar(status, STATUS_DESPESA, "despesa"));
        aplicarDespesa(despesa);
        return DespesaDTO.from(despesaRepository.save(despesa));
    }

    @Transactional
    public void excluirDespesa(Long id) {
        despesaRepository.delete(exigirDespesa(id));
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private void aplicarPagamento(Pagamento pagamento) {
        if ("PAGO".equals(pagamento.getStatus()) && pagamento.getPagoEm() == null) {
            pagamento.setPagoEm(OffsetDateTime.now());
        }
    }

    private void aplicarDespesa(Despesa despesa) {
        if ("PAGO".equals(despesa.getStatus()) && despesa.getPagoEm() == null) {
            despesa.setPagoEm(OffsetDateTime.now());
        }
    }

    private Pagamento exigirPagamento(Long id) {
        Long oficina = oficinaContexto.getOficinaId();
        return pagamentoRepository.findByIdAndOficinaId(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado: " + id));
    }

    private Despesa exigirDespesa(Long id) {
        Long oficina = oficinaContexto.getOficinaId();
        return despesaRepository.findByIdAndOficinaId(id, oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada: " + id));
    }

    private String normalizar(String status, Set<String> validos, String recurso) {
        String valor = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!validos.contains(valor)) {
            throw new RegraDeNegocioException(
                "Status inválido para " + recurso + ": " + status + ". Use " + String.join(", ", validos) + "."
            );
        }
        return valor;
    }

    private String limpar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
