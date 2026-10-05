package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.PecaDTO;
import com.salvo.salvo_api.dtos.ServicoDTO;
import com.salvo.salvo_api.dtos.UsuarioLogadoDTO;
import com.salvo.salvo_api.repositories.PecaRepository;
import com.salvo.salvo_api.repositories.ServicoRepository;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Serviços, Peças e Mecânicos")
@Transactional(readOnly = true)
public class CatalogoController {

    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OficinaContexto oficinaContexto;

    @GetMapping("/servicos")
    @Operation(summary = "Busca serviços no catálogo da oficina")
    public List<ServicoDTO> buscarServicos(@RequestParam(defaultValue = "") String q) {
        Long oficina = oficinaContexto.getOficinaId();
        return servicoRepository.buscar(oficina, q).stream().map(ServicoDTO::from).toList();
    }

    @GetMapping("/pecas")
    @Operation(summary = "Busca peças no catálogo da oficina")
    public List<PecaDTO> buscarPecas(@RequestParam(defaultValue = "") String q) {
        Long oficina = oficinaContexto.getOficinaId();
        return pecaRepository.buscar(oficina, q).stream().map(PecaDTO::from).toList();
    }

    @GetMapping("/mecanicos")
    @Operation(summary = "Lista todos os mecânicos e usuários da oficina")
    public List<UsuarioLogadoDTO> listarMecanicos() {
        Long oficina = oficinaContexto.getOficinaId();
        return usuarioRepository.findByOficinaIdAndAtivoTrue(oficina)
            .stream().map(UsuarioLogadoDTO::from).toList();
    }
}
