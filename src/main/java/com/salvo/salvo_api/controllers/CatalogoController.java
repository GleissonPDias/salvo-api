package com.salvo.salvo_api.controllers;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.dtos.AtualizarPecaDTO;
import com.salvo.salvo_api.dtos.AtualizarServicoDTO;
import com.salvo.salvo_api.dtos.CriarPecaDTO;
import com.salvo.salvo_api.dtos.CriarServicoDTO;
import com.salvo.salvo_api.dtos.MovimentacaoEstoqueDTO;
import com.salvo.salvo_api.dtos.MovimentacaoEstoqueRespostaDTO;
import com.salvo.salvo_api.dtos.PecaDTO;
import com.salvo.salvo_api.dtos.ServicoDTO;
import com.salvo.salvo_api.dtos.SugestoesServicoDTO;
import com.salvo.salvo_api.dtos.UsuarioLogadoDTO;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import com.salvo.salvo_api.services.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Serviços, Peças, Estoque e Mecânicos")
public class CatalogoController {

    private final CatalogoService catalogoService;
    private final UsuarioRepository usuarioRepository;
    private final OficinaContexto oficinaContexto;

    /* ------------------------------------------------------------------ */
    /* Serviços                                                            */
    /* ------------------------------------------------------------------ */

    @GetMapping("/servicos")
    @Operation(summary = "Busca serviços no catálogo da oficina (inclui inativos)")
    public List<ServicoDTO> buscarServicos(@RequestParam(defaultValue = "") String q) {
        return catalogoService.listarServicos(q);
    }

    @PostMapping("/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um serviço")
    public ServicoDTO criarServico(@RequestBody @Valid CriarServicoDTO dto) {
        return catalogoService.criarServico(dto);
    }

    @PutMapping("/servicos/{id}")
    @Operation(summary = "Atualiza um serviço")
    public ServicoDTO atualizarServico(@PathVariable Long id, @RequestBody @Valid AtualizarServicoDTO dto) {
        return catalogoService.atualizarServico(id, dto);
    }

    @DeleteMapping("/servicos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um serviço (exclusão lógica)")
    public void desativarServico(@PathVariable Long id) {
        catalogoService.desativarServico(id);
    }

    @GetMapping("/servicos/{id}/pecas-sugeridas")
    @Operation(summary = "Lista as peças sugeridas para um serviço")
    public List<PecaDTO> listarPecasSugeridas(@PathVariable Long id) {
        return catalogoService.listarPecasSugeridas(id);
    }

    @PutMapping("/servicos/{id}/pecas-sugeridas")
    @Operation(summary = "Substitui as peças sugeridas de um serviço")
    public List<PecaDTO> substituirPecasSugeridas(
        @PathVariable Long id,
        @RequestBody SugestoesServicoDTO dto
    ) {
        return catalogoService.substituirPecasSugeridas(id, dto.pecaIds());
    }

    /* ------------------------------------------------------------------ */
    /* Peças                                                               */
    /* ------------------------------------------------------------------ */

    @GetMapping("/pecas")
    @Operation(summary = "Busca peças no catálogo da oficina (inclui inativas)")
    public List<PecaDTO> buscarPecas(@RequestParam(defaultValue = "") String q) {
        return catalogoService.listarPecas(q);
    }

    @PostMapping("/pecas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma peça")
    public PecaDTO criarPeca(@RequestBody @Valid CriarPecaDTO dto) {
        return catalogoService.criarPeca(dto);
    }

    @PutMapping("/pecas/{id}")
    @Operation(summary = "Atualiza uma peça")
    public PecaDTO atualizarPeca(@PathVariable Long id, @RequestBody @Valid AtualizarPecaDTO dto) {
        return catalogoService.atualizarPeca(id, dto);
    }

    @DeleteMapping("/pecas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa uma peça (exclusão lógica)")
    public void desativarPeca(@PathVariable Long id) {
        catalogoService.desativarPeca(id);
    }

    /* ------------------------------------------------------------------ */
    /* Estoque                                                             */
    /* ------------------------------------------------------------------ */

    @PostMapping("/pecas/{id}/movimentacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma movimentação de estoque da peça")
    public MovimentacaoEstoqueRespostaDTO movimentar(
        @PathVariable Long id,
        @RequestBody @Valid MovimentacaoEstoqueDTO dto
    ) {
        return catalogoService.movimentar(id, dto);
    }

    @GetMapping("/pecas/{id}/movimentacoes")
    @Operation(summary = "Histórico de movimentações de uma peça")
    public List<MovimentacaoEstoqueRespostaDTO> movimentacoesDaPeca(
        @PathVariable Long id,
        @RequestParam(defaultValue = "100") int limite
    ) {
        return catalogoService.listarMovimentacoes(id, limite);
    }

    @GetMapping("/estoque/movimentacoes")
    @Operation(summary = "Histórico de movimentações de estoque da oficina")
    public List<MovimentacaoEstoqueRespostaDTO> movimentacoes(
        @RequestParam(defaultValue = "100") int limite
    ) {
        return catalogoService.listarMovimentacoes(null, limite);
    }

    /* ------------------------------------------------------------------ */
    /* Mecânicos                                                           */
    /* ------------------------------------------------------------------ */

    @GetMapping("/mecanicos")
    @Operation(summary = "Lista todos os mecânicos e usuários da oficina")
    @Transactional(readOnly = true)
    public List<UsuarioLogadoDTO> listarMecanicos() {
        Long oficina = oficinaContexto.getOficinaId();
        return usuarioRepository.findByOficinaIdAndAtivoTrue(oficina)
            .stream().map(UsuarioLogadoDTO::from).toList();
    }
}
