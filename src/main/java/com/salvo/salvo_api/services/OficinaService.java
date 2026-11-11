package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarOficinaDTO;
import com.salvo.salvo_api.dtos.OficinaDTO;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.repositories.OficinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OficinaService {

    private final OficinaRepository oficinaRepository;
    private final OficinaContexto oficinaContexto;
    private final PermissaoService permissaoService;

    @Transactional(readOnly = true)
    public OficinaDTO buscar() {
        return OficinaDTO.from(exigirOficina());
    }

    @Transactional
    public OficinaDTO atualizar(AtualizarOficinaDTO dto) {
        permissaoService.exigirAdmin();
        Oficina oficina = exigirOficina();

        oficina.setNome(dto.nome().trim());
        if (dto.nomeFantasia() != null) oficina.setNomeFantasia(limpar(dto.nomeFantasia()));
        if (dto.cnpj() != null) oficina.setCnpj(apenasDigitos(dto.cnpj()));
        if (dto.telefone() != null) oficina.setTelefone(limpar(dto.telefone()));
        if (dto.whatsapp() != null) oficina.setWhatsapp(limpar(dto.whatsapp()));
        if (dto.email() != null) oficina.setEmail(limpar(dto.email()));
        if (dto.site() != null) oficina.setSite(limpar(dto.site()));
        if (dto.logoUrl() != null) oficina.setLogoUrl(limpar(dto.logoUrl()));
        if (dto.cep() != null) oficina.setCep(limpar(dto.cep()));
        if (dto.logradouro() != null) oficina.setLogradouro(limpar(dto.logradouro()));
        if (dto.numero() != null) oficina.setNumero(limpar(dto.numero()));
        if (dto.complemento() != null) oficina.setComplemento(limpar(dto.complemento()));
        if (dto.bairro() != null) oficina.setBairro(limpar(dto.bairro()));
        if (dto.cidade() != null) oficina.setCidade(limpar(dto.cidade()));
        if (dto.uf() != null) oficina.setUf(limpar(dto.uf()) == null ? null : dto.uf().trim().toUpperCase());
        if (dto.horarioFuncionamento() != null) oficina.setHorarioFuncionamento(limpar(dto.horarioFuncionamento()));
        if (dto.validadeOrcamentoDias() != null) oficina.setValidadeOrcamentoDias(dto.validadeOrcamentoDias());
        if (dto.descontoMaximoPercentual() != null) oficina.setDescontoMaximoPercentual(dto.descontoMaximoPercentual());
        if (dto.corPrimaria() != null) oficina.setCorPrimaria(limpar(dto.corPrimaria()));

        return OficinaDTO.from(oficinaRepository.save(oficina));
    }

    private Oficina exigirOficina() {
        Long id = oficinaContexto.getOficinaId();
        return oficinaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Oficina não encontrada: " + id));
    }

    private String limpar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String apenasDigitos(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("\\D", "");
    }
}
