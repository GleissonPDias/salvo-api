package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarClienteDTO;
import com.salvo.salvo_api.dtos.ClienteDTO;
import com.salvo.salvo_api.dtos.CriarClienteDTO;
import com.salvo.salvo_api.entities.Cliente;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.repositories.ClienteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Page<ClienteDTO> listar(String q, Pageable pageable) {
        Long oficinaId = oficinaContexto.getOficinaId();
        if (q != null && !q.isBlank()) {
            return clienteRepository.buscar(oficinaId, q.trim(), pageable).map(ClienteDTO::from);
        }
        return clienteRepository.findByOficinaIdAndAtivoTrue(oficinaId, pageable).map(ClienteDTO::from);
    }

    @Transactional(readOnly = true)
    public ClienteDTO buscarPorId(Long id) {
        Long oficinaId = oficinaContexto.getOficinaId();
        return clienteRepository.findByOficinaIdAndId(oficinaId, id)
            .map(ClienteDTO::from)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    @Transactional
    public ClienteDTO criar(CriarClienteDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();

        Cliente cliente = new Cliente();
        cliente.setOficina(em.getReference(Oficina.class, oficinaId));
        cliente.setNome(dto.nome());
        cliente.setCpfCnpj(dto.cpfCnpj());
        cliente.setEmail(dto.email());
        cliente.setTelefone(dto.telefone());
        cliente.setTelefoneSecundario(dto.telefoneSecundario());
        cliente.setClassificacao(dto.classificacao());

        return ClienteDTO.from(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteDTO atualizar(Long id, AtualizarClienteDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));

        if (dto.nome() != null) cliente.setNome(dto.nome());
        if (dto.email() != null) cliente.setEmail(dto.email());
        if (dto.telefone() != null) cliente.setTelefone(dto.telefone());
        if (dto.telefoneSecundario() != null) cliente.setTelefoneSecundario(dto.telefoneSecundario());
        if (dto.classificacao() != null) cliente.setClassificacao(dto.classificacao());

        return ClienteDTO.from(clienteRepository.save(cliente));
    }

    @Transactional
    public void desativar(Long id) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }
}
