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
    private final BoxRepository boxRepository;
    private final UsuarioRepository usuarioRepository;
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

        var usuarioLogado = em.getReference(Usuario.class, usuario);

        OrdemServico os = new OrdemServico();
        os.setNumero(numero);
        os.setOficina(em.getReference(Oficina.class, oficina));
        os.setCliente(cliente);
        os.setVeiculo(veiculo);
        os.setStatus(statusAberta);
        os.setUsuarioAbertura(usuarioLogado);
        os.setAtualizadoPor(usuarioLogado);
        os.setPrioridade(dto.prioridade() != null ? dto.prioridade() : PrioridadeOs.NORMAL);
        os.setKmEntrada(dto.kmEntrada());
        os.setDiagnostico(dto.diagnostico());
        os.setObservacoes(dto.observacoes());
        os.setTipo(dto.tipo());
        os.setCanal(dto.canal());
        os.setTitulo(dto.titulo());
        os.setRelatoCliente(dto.relatoCliente());
        os.setObservacoesInternas(dto.observacoesInternas());
        os.setPrevisaoEntrega(dto.previsaoEntrega());
        os.setNivelCombustivel(dto.nivelCombustivel());
        os.setAprovacaoStatus("PENDENTE");

        if (dto.boxId() != null) {
            Box box = boxRepository.findByIdAndOficinaId(dto.boxId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Box não encontrado: " + dto.boxId()));
            os.setBox(box);
        }

        if (dto.mecanicoLiderId() != null) {
            Usuario lider = usuarioRepository.findByIdAndOficinaId(dto.mecanicoLiderId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Mecânico não encontrado: " + dto.mecanicoLiderId()));
            os.setMecanicoLider(lider);
        }

        OrdemServico salva = osRepository.save(os);
        registrarHistorico(salva, null, statusAberta, usuario, "OS #" + numero + " criada");

        em.flush();
        em.refresh(salva);
        return OrdemServicoDTO.from(salva);
    }

    @Transactional
    public OrdemServicoDTO atualizar(Integer numero, AtualizarOsDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        Long usuario = oficinaContexto.getUsuarioId();

        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numero)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numero));

        os.setAtualizadoPor(em.getReference(Usuario.class, usuario));

        if (dto.boxId() != null) {
            Box box = boxRepository.findByIdAndOficinaId(dto.boxId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Box não encontrado: " + dto.boxId()));
            os.setBox(box);
        } else {
            os.setBox(null);
        }

        if (dto.mecanicoLiderId() != null) {
            Usuario lider = usuarioRepository.findByIdAndOficinaId(dto.mecanicoLiderId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Mecânico não encontrado: " + dto.mecanicoLiderId()));
            os.setMecanicoLider(lider);
        } else {
            os.setMecanicoLider(null);
        }

        if (dto.prioridade() != null) os.setPrioridade(dto.prioridade());
        os.setKmEntrada(dto.kmEntrada());
        os.setKmSaida(dto.kmSaida());
        os.setDiagnostico(dto.diagnostico());
        if (dto.diagnosticoValidado() != null) os.setDiagnosticoValidado(dto.diagnosticoValidado());
        os.setObservacoes(dto.observacoes());
        os.setObservacoesInternas(dto.observacoesInternas());
        os.setObservacoesCliente(dto.observacoesCliente());
        os.setTipo(dto.tipo());
        os.setCanal(dto.canal());
        os.setTitulo(dto.titulo());
        os.setRelatoCliente(dto.relatoCliente());
        os.setPrevisaoEntrega(dto.previsaoEntrega());
        os.setNivelCombustivel(dto.nivelCombustivel());
        os.setFormaPagamento(dto.formaPagamento());
        if (dto.parcelas() != null) os.setParcelas(dto.parcelas());
        os.setGarantiaTipo(dto.garantiaTipo());
        os.setGarantiaDias(dto.garantiaDias());
        os.setGarantiaKm(dto.garantiaKm());
        os.setAprovacaoCanal(dto.aprovacaoCanal());
        os.setAprovacaoEnviadaEm(dto.aprovacaoEnviadaEm());
        os.setAprovacaoDecididaEm(dto.aprovacaoDecididaEm());
        os.setAprovacaoVisualizadaEm(dto.aprovacaoVisualizadaEm());
        os.setMotivoCancelamento(dto.motivoCancelamento());

        if (dto.aprovacaoStatus() != null) {
            String status = dto.aprovacaoStatus().toUpperCase();
            if (!List.of("PENDENTE", "APROVADO", "RECUSADO").contains(status)) {
                throw new RegraDeNegocioException("Status de aprovação inválido: " + dto.aprovacaoStatus());
            }
            os.setAprovacaoStatus(status);
        }

        if (dto.desconto() != null) os.setDesconto(dto.desconto());
        if (dto.descontoTipo() != null) os.setDescontoTipo(normalizarDescontoTipo(dto.descontoTipo()));

        OrdemServico atualizada = osRepository.save(os);
        em.flush();
        em.refresh(atualizada);
        return OrdemServicoDTO.from(atualizada);
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
        os.setAtualizadoPor(em.getReference(Usuario.class, usuario));

        String novoNome = novo.getNome();
        OffsetDateTime agora = OffsetDateTime.now();

        if ("PRONTA".equals(novoNome) || "EM_TESTE".equals(novoNome)) {
            os.setProntaEm(agora);
        }

        if ("ENTREGUE".equals(novoNome) || "CONCLUIDA".equals(novoNome)) {
            os.setEntregueEm(agora);
            os.setFechadaEm(agora);
        } else if ("CANCELADA".equals(novoNome)) {
            os.setCanceladaEm(agora);
            os.setFechadaEm(agora);
            if (dto.observacao() != null && !dto.observacao().isBlank()) {
                os.setMotivoCancelamento(dto.observacao());
            }
        } else {
            // Reabertura / retorno ao pátio: a OS deixa de estar encerrada.
            os.setFechadaEm(null);
            os.setEntregueEm(null);
            os.setCanceladaEm(null);
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
        item.setConcluido(Boolean.TRUE.equals(dto.concluido()));
        if (item.getConcluido()) item.setConcluidoEm(OffsetDateTime.now());
        if (dto.desconto() != null) item.setDesconto(dto.desconto());
        if (dto.descontoTipo() != null) item.setDescontoTipo(normalizarDescontoTipo(dto.descontoTipo()));

        if (dto.servicoId() != null) {
            Servico servico = servicoRepository.findByIdAndOficinaId(dto.servicoId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + dto.servicoId()));
            item.setServico(servico);
        }
        if (dto.mecanicoId() != null) {
            Usuario mecanico = usuarioRepository.findByIdAndOficinaId(dto.mecanicoId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mecânico não encontrado: " + dto.mecanicoId()));
            item.setMecanico(mecanico);
        }

        em.persist(item);
        return OsServicoDTO.from(item);
    }

    @Transactional
    public OsServicoDTO atualizarServico(Integer numeroOs, Long itemId, AtualizarOsServicoDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numeroOs)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numeroOs));

        OsServico item = os.getServicos().stream()
            .filter(s -> s.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado: " + itemId));

        item.setDescricao(dto.descricao());
        item.setQuantidade(dto.quantidade());
        item.setValorUnitario(dto.valorUnitario());
        item.setTempoEstimadoHoras(dto.tempoEstimadoHoras());

        if (dto.servicoId() != null) {
            Servico servico = servicoRepository.findByIdAndOficinaId(dto.servicoId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + dto.servicoId()));
            item.setServico(servico);
        } else {
            item.setServico(null);
        }

        if (dto.mecanicoId() != null) {
            Usuario mecanico = usuarioRepository.findByIdAndOficinaId(dto.mecanicoId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mecânico não encontrado: " + dto.mecanicoId()));
            item.setMecanico(mecanico);
        } else {
            item.setMecanico(null);
        }

        if (dto.concluido() != null) {
            boolean concluido = dto.concluido();
            item.setConcluido(concluido);
            item.setConcluidoEm(concluido ? OffsetDateTime.now() : null);
        }
        if (dto.desconto() != null) item.setDesconto(dto.desconto());
        if (dto.descontoTipo() != null) item.setDescontoTipo(normalizarDescontoTipo(dto.descontoTipo()));

        em.flush();
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
        item.setCotada(dto.cotada() == null || dto.cotada());
        item.setInstalada(Boolean.TRUE.equals(dto.instalada()));
        if (item.getInstalada()) item.setInstaladaEm(OffsetDateTime.now());
        if (dto.desconto() != null) item.setDesconto(dto.desconto());
        if (dto.descontoTipo() != null) item.setDescontoTipo(normalizarDescontoTipo(dto.descontoTipo()));
        item.setUnidade(dto.unidade());

        if (dto.pecaId() != null) {
            Peca peca = pecaRepository.findByIdAndOficinaId(dto.pecaId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + dto.pecaId()));
            item.setPeca(peca);
        }

        em.persist(item);
        return OsPecaDTO.from(item);
    }

    @Transactional
    public OsPecaDTO atualizarPeca(Integer numeroOs, Long itemId, AtualizarOsPecaDTO dto) {
        Long oficina = oficinaContexto.getOficinaId();
        OrdemServico os = osRepository.findByOficinaIdAndNumero(oficina, numeroOs)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: #" + numeroOs));

        OsPeca item = os.getPecas().stream()
            .filter(p -> p.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado: " + itemId));

        item.setDescricao(dto.descricao());
        item.setQuantidade(dto.quantidade());
        item.setValorUnitario(dto.valorUnitario());

        if (dto.pecaId() != null) {
            Peca peca = pecaRepository.findByIdAndOficinaId(dto.pecaId(), oficina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + dto.pecaId()));
            item.setPeca(peca);
        } else {
            item.setPeca(null);
        }

        if (dto.cotada() != null) item.setCotada(dto.cotada());
        if (dto.instalada() != null) {
            boolean instalada = dto.instalada();
            item.setInstalada(instalada);
            item.setInstaladaEm(instalada ? OffsetDateTime.now() : null);
        }
        if (dto.desconto() != null) item.setDesconto(dto.desconto());
        if (dto.descontoTipo() != null) item.setDescontoTipo(normalizarDescontoTipo(dto.descontoTipo()));
        item.setUnidade(dto.unidade());

        em.flush();
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

    private String normalizarDescontoTipo(String value) {
        String tipo = value == null ? "" : value.trim().toUpperCase();
        if (!List.of("VALOR", "PERCENTUAL").contains(tipo)) {
            throw new RegraDeNegocioException("Tipo de desconto inválido: " + value);
        }
        return tipo;
    }
}
