package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.domain.os.PrioridadeOs;
import com.salvo.salvo_api.dtos.*;
import com.salvo.salvo_api.entities.*;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdemServicoService {

    private final OrdemServicoRepository osRepository;
    private final StatusOrdemServicoRepository statusRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Page<OrdemServicoResumoDTO> listar(String status, Long clienteId, Pageable pageable) {
        Long oficina = oficinaContexto.getOficinaId();
        if (status != null && !status.isBlank()) {
            return osRepository.findByOficinaAndStatus(oficina, status, pageable)
                .map(OrdemServicoResumoDTO::from);
        }
        if (clienteId != null) {
            return osRepository.findByOficinaAndCliente(oficina, clienteId, pageable)
                .map(OrdemServicoResumoDTO::from);
        }
        return osRepository.findByOficinaIdOrderByNumeroDesc(oficina, pageable)
            .map(OrdemServicoResumoDTO::from);
    }

    @Transactional(readOnly = true)
    public OrdemServicoDTO buscarPorNumero(Integer numero) {
        Long oficina = oficinaContexto.getOficinaId();
        return osRepository.findByOficinaIdAndNumero(oficina, numero)
            .map(OrdemServicoDTO::from)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numero));
    }

    @Transactional
    public OrdemServicoDTO criar(CriarOsDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        Long usuario = oficinaContexto.getUsuarioId();

        var cliente = clienteRepository.findByOficinaIdAndId(oficina, dto.clienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + dto.clienteId()));

        var veiculo = veiculoRepository.findByIdAndOficina(dto.veiculoId(), oficina)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + dto.veiculoId()));

        if (!veiculo.getCliente().getId().equals(dto.clienteId())) {
            throw new RegraDeNegocioException("Veículo não pertence ao cliente informado");
        }

        // Gera o próximo número de forma atômica via função PostgreSQL da V3
        Integer numero = (Integer) em
            .createNativeQuery("SELECT fn_proximo_numero_ordem_servico(:oficina)")
            .setParameter("oficina", oficina)
            .getSingleResult();

        StatusOrdemServico statusAberta = statusRepository.findByNome("ABERTA")
            .orElseThrow(() -> new IllegalStateException("Status 'ABERTA' não encontrado. Execute os seeds."));

        OrdemServico os = new OrdemServico();
        os.setNumero(numero);
        os.setOficina(em.getReference(Oficina.class, oficina));
        os.setCliente(cliente);
        os.setVeiculo(veiculo);
        os.setStatus(statusAberta);
        os.setUsuarioAbertura(em.getReference(Usuario.class, usuario));
        os.setPrioridade(dto.prioridade() != null ? dto.prioridade() : PrioridadeOs.NORMAL);
        os.setKmEntrada(dto.kmEntrada());
        os.setDiagnostico(dto.diagnostico());
        os.setObservacoes(dto.observacoes());

        if (dto.mecanicoLiderId() != null) {
            os.setMecanicoLider(em.getReference(Usuario.class, dto.mecanicoLiderId()));
        }

        OrdemServico salva = osRepository.save(os);
        registrarHistorico(salva, null, statusAberta, usuario, "OS #" + numero + " criada");

        em.flush();
        em.refresh(salva);
        return OrdemServicoDTO.from(salva);
    }

    @Transactional
    public OrdemServicoDTO alterarStatus(Integer numero, AlterarStatusOsDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        Long usuario = oficinaContexto.getUsuarioId();

        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numero)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numero));

        StatusOrdemServico anterior = os.getStatus();
        StatusOrdemServico novo = statusRepository.findByNome(dto.status())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Status inválido: " + dto.status()));

        if (anterior.getId().equals(novo.getId())) {
            throw new RegraDeNegocioException("A OS já está no status " + dto.status());
        }

        os.setStatus(novo);

        if ("CONCLUIDA".equals(dto.status()) || "CANCELADA".equals(dto.status())) {
            os.setFechadaEm(OffsetDateTime.now());
        }

        String descricao = (dto.observacao() != null && !dto.observacao().isBlank())
            ? dto.observacao()
            : "Status alterado de " + anterior.getNome() + " para " + novo.getNome();

        registrarHistorico(os, anterior, novo, usuario, descricao);

        OrdemServico atualizada = osRepository.save(os);
        em.flush();
        em.refresh(atualizada);
        return OrdemServicoDTO.from(atualizada);
    }

    @Transactional
    public OsServicoDTO adicionarServico(Integer numeroOs, AdicionarServicoOsDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numeroOs)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numeroOs));

        OsServico item = new OsServico();
        item.setOrdemServico(os);
        item.setDescricao(dto.descricao());
        item.setQuantidade(dto.quantidade());
        item.setValorUnitario(dto.valorUnitario());
        item.setTempoEstimadoHoras(dto.tempoEstimadoHoras());

        if (dto.servicoId() != null) {
            Servico servico = servicoRepository.findByIdAndOficinaId(dto.servicoId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + dto.servicoId()));
            item.setServico(servico);
        }
        if (dto.mecanicoId() != null) {
            item.setMecanico(em.getReference(Usuario.class, dto.mecanicoId()));
        }

        em.persist(item);
        return OsServicoDTO.from(item);
    }

    @Transactional
    public void removerServico(Integer numeroOs, Long itemId) {
        Long oficina = oficinaContexto.getOficinaId();
        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numeroOs)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numeroOs));

        OsServico item = os.getServicos().stream()
            .filter(s -> s.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado: " + itemId));

        os.getServicos().remove(item);
        osRepository.save(os);
    }

    @Transactional
    public OsPecaDTO adicionarPeca(Integer numeroOs, AdicionarPecaOsDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numeroOs)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numeroOs));

        OsPeca item = new OsPeca();
        item.setOrdemServico(os);
        item.setDescricao(dto.descricao());
        item.setQuantidade(dto.quantidade());
        item.setValorUnitario(dto.valorUnitario());

        if (dto.pecaId() != null) {
            Peca peca = pecaRepository.findByIdAndOficinaId(dto.pecaId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + dto.pecaId()));
            item.setPeca(peca);
        }

        em.persist(item);
        return OsPecaDTO.from(item);
    }

    @Transactional
    public void removerPeca(Integer numeroOs, Long itemId) {
        Long oficina = oficinaContexto.getOficinaId();
        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numeroOs)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numeroOs));

        OsPeca item = os.getPecas().stream()
            .filter(p -> p.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado: " + itemId));

        os.getPecas().remove(item);
        osRepository.save(os);
    }

    @Transactional(readOnly = true)
    public List<OsHistoricoDTO> listarHistorico(Integer numero) {
        Long oficina = oficinaContexto.getOficinaId();
        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numero)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numero));

        return os.getHistorico().stream()
            .sorted((a, b) -> a.getCriadoEm().compareTo(b.getCriadoEm()))
            .map(OsHistoricoDTO::from)
            .toList();
    }

    private void registrarHistorico(OrdemServico os, StatusOrdemServico anterior,
                                    StatusOrdemServico novo, Long usuarioId, String descricao) {
        OsHistorico h = new OsHistorico();
        h.setOrdemServico(os);
        h.setStatusAnterior(anterior);
        h.setStatusNovo(novo);
        h.setDescricao(descricao);
        if (usuarioId != null) {
            h.setUsuario(em.getReference(Usuario.class, usuarioId));
        }
        em.persist(h);
    }
}
