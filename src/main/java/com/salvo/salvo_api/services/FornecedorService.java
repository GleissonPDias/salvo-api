package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarFornecedorDTO;
import com.salvo.salvo_api.dtos.CriarFornecedorDTO;
import com.salvo.salvo_api.dtos.FornecedorDTO;
import com.salvo.salvo_api.entities.Fornecedor;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.repositories.FornecedorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final OficinaContexto oficinaContexto;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<FornecedorDTO> listar(boolean incluirInativos) {
        Long oficinaId = oficinaContexto.getOficinaId();
        List<Fornecedor> lista = incluirInativos
            ? fornecedorRepository.findByOficinaIdOrderByNome(oficinaId)
            : fornecedorRepository.findByOficinaIdAndAtivoTrueOrderByNome(oficinaId);
        return lista.stream().map(FornecedorDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public FornecedorDTO buscar(Long id) {
        return FornecedorDTO.from(exigirFornecedor(id));
    }

    @Transactional
    public FornecedorDTO criar(CriarFornecedorDTO dto) {
        Long oficinaId = oficinaContexto.getOficinaId();

        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setOficina(em.getReference(Oficina.class, oficinaId));
        fornecedor.setNome(dto.nome().trim());
        fornecedor.setRazaoSocial(limpar(dto.razaoSocial()));
        fornecedor.setCnpj(apenasDigitos(dto.cnpj()));
        fornecedor.setEmail(limpar(dto.email()));
        fornecedor.setTelefone(limpar(dto.telefone()));
        fornecedor.setContato(limpar(dto.contato()));
        fornecedor.setLeadTimeDias(dto.leadTimeDias() != null ? dto.leadTimeDias() : 1);
        fornecedor.setEndereco(limpar(dto.endereco()));
        fornecedor.setObservacoes(limpar(dto.observacoes()));
        fornecedor.setAtivo(dto.ativo() == null || dto.ativo());

        return FornecedorDTO.from(fornecedorRepository.save(fornecedor));
    }

    @Transactional
    public FornecedorDTO atualizar(Long id, AtualizarFornecedorDTO dto) {
        Fornecedor fornecedor = exigirFornecedor(id);

        if (dto.nome() != null && !dto.nome().isBlank()) fornecedor.setNome(dto.nome().trim());
        if (dto.razaoSocial() != null) fornecedor.setRazaoSocial(limpar(dto.razaoSocial()));
        if (dto.cnpj() != null) fornecedor.setCnpj(apenasDigitos(dto.cnpj()));
        if (dto.email() != null) fornecedor.setEmail(limpar(dto.email()));
        if (dto.telefone() != null) fornecedor.setTelefone(limpar(dto.telefone()));
        if (dto.contato() != null) fornecedor.setContato(limpar(dto.contato()));
        if (dto.leadTimeDias() != null) fornecedor.setLeadTimeDias(dto.leadTimeDias());
        if (dto.endereco() != null) fornecedor.setEndereco(limpar(dto.endereco()));
        if (dto.observacoes() != null) fornecedor.setObservacoes(limpar(dto.observacoes()));
        if (dto.ativo() != null) fornecedor.setAtivo(dto.ativo());

        return FornecedorDTO.from(fornecedorRepository.save(fornecedor));
    }

    @Transactional
    public void desativar(Long id) {
        Fornecedor fornecedor = exigirFornecedor(id);
        fornecedor.setAtivo(false);
        fornecedorRepository.save(fornecedor);
    }

    private Fornecedor exigirFornecedor(Long id) {
        Long oficinaId = oficinaContexto.getOficinaId();
        return fornecedorRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
    }

    private String limpar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String apenasDigitos(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("\\D", "");
    }
}
