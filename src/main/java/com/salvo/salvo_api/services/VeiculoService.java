package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarVeiculoDTO;
import com.salvo.salvo_api.dtos.CriarVeiculoDTO;
import com.salvo.salvo_api.dtos.VeiculoDTO;
import com.salvo.salvo_api.entities.Cliente;
import com.salvo.salvo_api.entities.Veiculo;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.repositories.ClienteRepository;
import com.salvo.salvo_api.repositories.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final OficinaContexto oficinaContexto;

    @Transactional(readOnly = true)
    public Page<VeiculoDTO> listar(Pageable pageable, boolean incluirInativos) {
        Long oficinaId = oficinaContexto.getOficinaId();
        if (incluirInativos) {
            return veiculoRepository.findAllByOficina(oficinaId, pageable).map(VeiculoDTO::from);
        }
        return veiculoRepository.findByOficina(oficinaId, pageable).map(VeiculoDTO::from);
    }

    @Transactional(readOnly = true)
    public List<VeiculoDTO> listarPorCliente(Long clienteId) {
        Long oficinaId = oficinaContexto.getOficinaId();
        clienteRepository.findByOficinaIdAndId(oficinaId, clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + clienteId));
        return veiculoRepository.findByClienteIdAndAtivoTrue(clienteId)
            .stream().map(VeiculoDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public VeiculoDTO buscarPorId(Long id) {
        Long oficinaId = oficinaContexto.getOficinaId();
        return veiculoRepository.findByIdAndOficina(id, oficinaId)
            .map(VeiculoDTO::from)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + id));
    }

    @Transactional
    public VeiculoDTO criar(Long clienteId, CriarVeiculoDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + clienteId));

        Veiculo veiculo = new Veiculo();
        veiculo.setCliente(cliente);
        veiculo.setOficina(cliente.getOficina());
        veiculo.setNumero(veiculoRepository.maxNumero(oficinaId) + 1);
        veiculo.setPlaca(dto.placa().toUpperCase().replace("-", "").replace(" ", ""));
        veiculo.setChassi(dto.chassi());
        veiculo.setMarca(dto.marca());
        veiculo.setModelo(dto.modelo());
        veiculo.setVersao(dto.versao());
        veiculo.setAnoFabricacao(dto.anoFabricacao());
        veiculo.setAnoModelo(dto.anoModelo());
        veiculo.setCor(dto.cor());
        veiculo.setCombustivel(dto.combustivel());
        veiculo.setMotorizacao(dto.motorizacao());
        veiculo.setTransmissao(dto.transmissao());
        veiculo.setKmAtual(dto.kmAtual() != null ? dto.kmAtual() : 0);
        veiculo.setFotoUrl(dto.fotoUrl());
        veiculo.setCategoria(dto.categoria());
        veiculo.setRascunho(Boolean.TRUE.equals(dto.rascunho()));

        Veiculo salvo = veiculoRepository.save(veiculo);
        return VeiculoDTO.from(salvo);
    }

    @Transactional
    public VeiculoDTO atualizar(Long id, AtualizarVeiculoDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Veiculo veiculo = veiculoRepository.findByIdAndOficinaIncluindoInativos(id, oficinaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + id));

        if (dto.placa() != null) veiculo.setPlaca(dto.placa());
        if (dto.marca() != null) veiculo.setMarca(dto.marca());
        if (dto.modelo() != null) veiculo.setModelo(dto.modelo());
        if (dto.cor() != null) veiculo.setCor(dto.cor());
        if (dto.combustivel() != null) veiculo.setCombustivel(dto.combustivel());
        if (dto.motorizacao() != null) veiculo.setMotorizacao(dto.motorizacao());
        if (dto.transmissao() != null) veiculo.setTransmissao(dto.transmissao());
        if (dto.kmAtual() != null) veiculo.setKmAtual(dto.kmAtual());
        if (dto.versao() != null) veiculo.setVersao(dto.versao());
        if (dto.fotoUrl() != null) veiculo.setFotoUrl(dto.fotoUrl());
        if (dto.categoria() != null) veiculo.setCategoria(dto.categoria());
        if (dto.chassi() != null) veiculo.setChassi(dto.chassi());
        if (dto.anoFabricacao() != null) veiculo.setAnoFabricacao(dto.anoFabricacao());
        if (dto.anoModelo() != null) veiculo.setAnoModelo(dto.anoModelo());
        if (dto.rascunho() != null) veiculo.setRascunho(dto.rascunho());
        if (dto.ativo() != null) veiculo.setAtivo(dto.ativo());

        return VeiculoDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public void desativar(Long id) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Veiculo veiculo = veiculoRepository.findByIdAndOficina(id, oficinaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + id));
        veiculo.setAtivo(false);
        veiculoRepository.save(veiculo);
    }
}
