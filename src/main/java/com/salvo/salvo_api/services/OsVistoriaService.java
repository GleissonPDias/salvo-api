package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.OsVistoriaDTO;
import com.salvo.salvo_api.dtos.RegistrarVistoriaDTO;
import com.salvo.salvo_api.dtos.VistoriaDanoDTO;
import com.salvo.salvo_api.dtos.VistoriaFotoDTO;
import com.salvo.salvo_api.entities.OrdemServico;
import com.salvo.salvo_api.entities.OsVistoria;
import com.salvo.salvo_api.entities.OsVistoriaDano;
import com.salvo.salvo_api.entities.OsVistoriaFoto;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.OrdemServicoRepository;
import com.salvo.salvo_api.repositories.OsVistoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OsVistoriaService {

    private final OsVistoriaRepository vistoriaRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final OficinaContexto oficinaContexto;

    @Transactional(readOnly = true)
    public OsVistoriaDTO buscarPorNumero(Integer numero) {
        OrdemServico ordem = exigirOrdem(numero);
        return vistoriaRepository.findByOrdemServicoId(ordem.getId())
            .map(OsVistoriaDTO::from)
            .orElseGet(() -> OsVistoriaDTO.vazia(ordem.getId()));
    }

    @Transactional
    public OsVistoriaDTO salvar(Integer numero, RegistrarVistoriaDTO dto) {
        OrdemServico ordem = exigirOrdem(numero);
        Long usuarioId = oficinaContexto.getUsuarioId();

        OsVistoria vistoria = vistoriaRepository.findByOrdemServicoId(ordem.getId()).orElseGet(() -> {
            OsVistoria nova = new OsVistoria();
            nova.setOrdemServico(ordem);
            return nova;
        });

        vistoria.setClienteCiente(Boolean.TRUE.equals(dto.clienteCiente()));
        vistoria.setPertences(limpar(dto.pertences()));
        vistoria.setObservacoes(limpar(dto.observacoes()));
        vistoria.setRealizadaEm(dto.realizadaEm() != null ? dto.realizadaEm() : OffsetDateTime.now());
        vistoria.setRealizadaPor(usuarioId);

        vistoria.getDanos().clear();
        if (dto.danos() != null) {
            Set<String> vistos = new HashSet<>();
            for (VistoriaDanoDTO danoDto : dto.danos()) {
                String chave = (danoDto.area().trim() + "|" + danoDto.tipo().trim()).toUpperCase();
                if (!vistos.add(chave)) {
                    throw new RegraDeNegocioException(
                        "Dano duplicado para a área " + danoDto.area() + " e tipo " + danoDto.tipo() + "."
                    );
                }
                OsVistoriaDano dano = new OsVistoriaDano();
                dano.setVistoria(vistoria);
                dano.setArea(danoDto.area().trim());
                dano.setTipo(danoDto.tipo().trim());
                dano.setObservacao(limpar(danoDto.observacao()));
                vistoria.getDanos().add(dano);
            }
        }

        vistoria.getFotos().clear();
        if (dto.fotos() != null) {
            for (VistoriaFotoDTO fotoDto : dto.fotos()) {
                OsVistoriaFoto foto = new OsVistoriaFoto();
                foto.setVistoria(vistoria);
                foto.setUrl(fotoDto.url().trim());
                foto.setStoragePath(limpar(fotoDto.storagePath()));
                foto.setLegenda(limpar(fotoDto.legenda()));
                vistoria.getFotos().add(foto);
            }
        }

        return OsVistoriaDTO.from(vistoriaRepository.saveAndFlush(vistoria));
    }

    private OrdemServico exigirOrdem(Integer numero) {
        Long oficina = oficinaContexto.getOficinaId();
        return ordemServicoRepository.findByOficinaIdAndNumero(oficina, numero)
            .orElseThrow(() -> new RecursoNaoEncontradoException("OS não encontrada: " + numero));
    }

    private String limpar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
