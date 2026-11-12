package com.salvo.salvo_api.services;

import com.salvo.salvo_api.configs.OficinaContexto;
import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.dtos.AtualizarUsuarioDTO;
import com.salvo.salvo_api.dtos.CriarUsuarioDTO;
import com.salvo.salvo_api.dtos.MecanicoDTO;
import com.salvo.salvo_api.dtos.RedefinirSenhaResponseDTO;
import com.salvo.salvo_api.dtos.UsuarioCriadoDTO;
import com.salvo.salvo_api.dtos.UsuarioDTO;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.entities.UsuarioPermissao;
import com.salvo.salvo_api.exceptions.AcessoNegadoException;
import com.salvo.salvo_api.exceptions.RecursoNaoEncontradoException;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.OficinaRepository;
import com.salvo.salvo_api.repositories.UsuarioPermissaoRepository;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String ALFABETO_SENHA =
        "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final UsuarioPermissaoRepository usuarioPermissaoRepository;
    private final OficinaRepository oficinaRepository;
    private final OficinaContexto oficinaContexto;
    private final SupabaseAdminService supabaseAdminService;
    private final PermissaoService permissaoService;

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar(String busca, Boolean ativo, Boolean executaOs) {
        Long oficinaId = oficinaContexto.getOficinaId();
        String termo = normalizarBusca(busca);

        List<Usuario> usuarios = usuarioRepository.buscarEquipe(oficinaId, termo, ativo, executaOs);
        Map<Long, List<ModuloPermissao>> permissoes = permissoesPorUsuario(usuarios);

        return usuarios.stream()
            .map(usuario -> UsuarioDTO.from(usuario, permissoesDo(usuario, permissoes)))
            .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscar(Long id) {
        Long oficinaId = oficinaContexto.getOficinaId();
        Usuario usuario = buscarDaOficina(id, oficinaId);
        return UsuarioDTO.from(usuario, permissaoService.modulosDoUsuario(usuario));
    }

    @Transactional(readOnly = true)
    public List<MecanicoDTO> listarMecanicos() {
        Long oficinaId = oficinaContexto.getOficinaId();
        return usuarioRepository
            .findByOficinaIdAndAtivoTrueAndExecutaOsTrueOrderByLiderDescNomeAsc(oficinaId)
            .stream()
            .map(MecanicoDTO::from)
            .toList();
    }

    /**
     * Cria a identidade no Supabase Auth e o usuário local.
     *
     * Não usa @Transactional de propósito: a chamada HTTP ao Supabase não deve
     * segurar uma transação/connection do banco. Se o insert falhar, o usuário
     * criado no Auth é removido como compensação.
     */
    public UsuarioCriadoDTO criar(CriarUsuarioDTO dto) {
        permissaoService.exigirAdmin();

        Usuario admin = oficinaContexto.getUsuarioLogado();
        Long oficinaId = admin.getOficina().getId();
        String email = normalizarEmail(dto.email());

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraDeNegocioException("E-mail já cadastrado");
        }

        PerfilUsuario perfil = dto.perfil() != null ? dto.perfil() : PerfilUsuario.FUNCIONARIO;

        boolean enviarConvite = Boolean.TRUE.equals(dto.enviarConvite());
        boolean senhaGerada = false;
        String senha = dto.senhaInicial();

        if (!enviarConvite && (senha == null || senha.isBlank())) {
            senha = gerarSenhaTemporaria();
            senhaGerada = true;
        }

        Map<String, Object> metadata = Map.of(
            "nome", dto.nome().trim(),
            "oficina", admin.getOficina().getNome(),
            "perfil", perfil.name()
        );

        var authUserId = enviarConvite
            ? supabaseAdminService.convidarUsuario(email, metadata)
            : supabaseAdminService.criarUsuario(email, senha, metadata);

        Usuario usuario = new Usuario();
        usuario.setOficina(oficinaRepository.getReferenceById(oficinaId));
        usuario.setAuthUserId(authUserId);
        usuario.setNome(dto.nome().trim());
        usuario.setEmail(email);
        usuario.setCargo(dto.cargo().trim());
        usuario.setPerfil(perfil);
        usuario.setDono(false);
        usuario.setExecutaOs(dto.executaOs() == null || dto.executaOs());
        usuario.setDeveTrocarSenha(!enviarConvite);
        usuario.setTelefone(limpar(dto.telefone()));
        usuario.setEspecialidades(limpar(dto.especialidades()));
        usuario.setLider(Boolean.TRUE.equals(dto.lider()));
        usuario.setComissaoPercentual(
            dto.comissaoPercentual() != null ? dto.comissaoPercentual() : BigDecimal.ZERO
        );
        usuario.setCorAgenda(limpar(dto.corAgenda()));
        usuario.setAtivo(true);

        try {
            Usuario salvo = usuarioRepository.save(usuario);
            List<ModuloPermissao> permissoes = substituirPermissoes(salvo, dto.permissoes());
            return new UsuarioCriadoDTO(
                UsuarioDTO.from(salvo, permissoes),
                senhaGerada ? senha : null
            );
        } catch (RuntimeException ex) {
            if (usuario.getId() != null) {
                try {
                    usuarioRepository.deleteById(usuario.getId());
                } catch (RuntimeException ignored) {
                    // A falha original é mais relevante para o usuário.
                }
            }
            try {
                supabaseAdminService.excluirUsuario(authUserId);
            } catch (RuntimeException ignored) {
                // A falha original do banco é mais relevante para o usuário.
            }
            throw ex;
        }
    }

    @Transactional
    public UsuarioDTO atualizar(Long id, AtualizarUsuarioDTO dto) {
        permissaoService.exigirAdmin();

        Usuario logado = oficinaContexto.getUsuarioLogado();
        Long oficinaId = logado.getOficina().getId();
        Usuario usuario = buscarDaOficina(id, oficinaId);
        garantirPodeGerenciar(usuario, logado);

        if (dto.nome() != null) {
            if (dto.nome().isBlank()) {
                throw new RegraDeNegocioException("Nome não pode ficar em branco");
            }
            usuario.setNome(dto.nome().trim());
        }
        if (dto.cargo() != null) {
            if (dto.cargo().isBlank()) {
                throw new RegraDeNegocioException("Cargo não pode ficar em branco");
            }
            usuario.setCargo(dto.cargo().trim());
        }
        if (dto.telefone() != null) usuario.setTelefone(limpar(dto.telefone()));
        if (dto.especialidades() != null) usuario.setEspecialidades(limpar(dto.especialidades()));
        if (dto.lider() != null) usuario.setLider(dto.lider());
        if (dto.comissaoPercentual() != null) usuario.setComissaoPercentual(dto.comissaoPercentual());
        if (dto.corAgenda() != null) usuario.setCorAgenda(limpar(dto.corAgenda()));
        if (dto.executaOs() != null) usuario.setExecutaOs(dto.executaOs());

        if (dto.perfil() != null && dto.perfil() != usuario.getPerfil()) {
            if (Boolean.TRUE.equals(usuario.getDono())) {
                throw new RegraDeNegocioException("O perfil do dono da oficina não pode ser alterado");
            }
            if (usuario.getId().equals(logado.getId()) && dto.perfil() != PerfilUsuario.ADMIN) {
                throw new RegraDeNegocioException(
                    "Você não pode remover o seu próprio acesso de administrador"
                );
            }
            usuario.setPerfil(dto.perfil());
        }

        Usuario salvo = usuarioRepository.save(usuario);
        List<ModuloPermissao> permissoes = salvo.getPerfil() == PerfilUsuario.ADMIN
            ? List.of(ModuloPermissao.values())
            : substituirPermissoes(salvo, dto.permissoes());

        return UsuarioDTO.from(salvo, permissoes);
    }

    public UsuarioDTO alterarStatus(Long id, Boolean ativo) {
        permissaoService.exigirAdmin();
        Usuario admin = oficinaContexto.getUsuarioLogado();
        Long oficinaId = admin.getOficina().getId();
        Usuario usuario = buscarDaOficina(id, oficinaId);
        garantirPodeGerenciar(usuario, admin);

        if (!ativo && usuario.getId().equals(admin.getId())) {
            throw new RegraDeNegocioException("Você não pode desativar o seu próprio usuário");
        }
        if (!ativo && Boolean.TRUE.equals(usuario.getDono())) {
            throw new RegraDeNegocioException("O dono da oficina não pode ser desativado");
        }
        if (!ativo
            && usuario.getPerfil() == PerfilUsuario.ADMIN
            && usuarioRepository.countByOficinaIdAndPerfilAndAtivoTrue(oficinaId, PerfilUsuario.ADMIN) <= 1) {
            throw new RegraDeNegocioException("Não é possível desativar o último administrador ativo da oficina");
        }

        usuario.setAtivo(ativo);
        Usuario salvo = usuarioRepository.save(usuario);
        supabaseAdminService.definirBanimento(salvo.getAuthUserId(), !ativo);
        return UsuarioDTO.from(salvo, permissaoService.modulosDoUsuario(salvo));
    }

    public RedefinirSenhaResponseDTO redefinirSenha(Long id) {
        permissaoService.exigirAdmin();

        Usuario admin = oficinaContexto.getUsuarioLogado();
        Long oficinaId = admin.getOficina().getId();
        Usuario usuario = buscarDaOficina(id, oficinaId);
        garantirPodeGerenciar(usuario, admin);

        if (usuario.getAuthUserId() == null) {
            throw new RegraDeNegocioException(
                "Este usuário não possui identidade no Supabase Auth para redefinir a senha"
            );
        }

        String senha = gerarSenhaTemporaria();
        supabaseAdminService.atualizarSenha(usuario.getAuthUserId(), senha);

        usuario.setDeveTrocarSenha(true);
        usuarioRepository.save(usuario);
        return new RedefinirSenhaResponseDTO(senha);
    }

    private Usuario buscarDaOficina(Long id, Long oficinaId) {
        return usuarioRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }

    /**
     * Só o dono pode gerenciar a própria conta de dono. Os demais
     * administradores não podem alterar, desativar ou redefinir a senha dele.
     */
    private void garantirPodeGerenciar(Usuario alvo, Usuario logado) {
        if (Boolean.TRUE.equals(alvo.getDono()) && !Boolean.TRUE.equals(logado.getDono())) {
            throw new AcessoNegadoException(
                "Somente o dono da oficina pode gerenciar a própria conta"
            );
        }
    }

    private List<ModuloPermissao> substituirPermissoes(Usuario usuario, List<ModuloPermissao> permissoes) {
        usuarioPermissaoRepository.deleteByUsuarioId(usuario.getId());

        if (usuario.getPerfil() == PerfilUsuario.ADMIN) {
            return List.of(ModuloPermissao.values());
        }

        Set<ModuloPermissao> normalizadas = permissoes == null
            ? EnumSet.noneOf(ModuloPermissao.class)
            : permissoes.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(
                    () -> EnumSet.noneOf(ModuloPermissao.class)
                ));

        if (normalizadas.isEmpty()) {
            return List.of();
        }

        List<UsuarioPermissao> registros = normalizadas.stream()
            .map(modulo -> {
                UsuarioPermissao registro = new UsuarioPermissao();
                registro.setUsuarioId(usuario.getId());
                registro.setModulo(modulo);
                return registro;
            })
            .toList();

        usuarioPermissaoRepository.saveAll(registros);
        return normalizadas.stream().sorted().toList();
    }

    private Map<Long, List<ModuloPermissao>> permissoesPorUsuario(List<Usuario> usuarios) {
        List<Long> ids = usuarios.stream().map(Usuario::getId).toList();
        if (ids.isEmpty()) return Map.of();

        return usuarioPermissaoRepository.buscarPorUsuarios(ids).stream()
            .collect(groupingBy(UsuarioPermissao::getUsuarioId, mapping(UsuarioPermissao::getModulo, toList())));
    }

    private List<ModuloPermissao> permissoesDo(
        Usuario usuario,
        Map<Long, List<ModuloPermissao>> cache
    ) {
        if (usuario.getPerfil() == PerfilUsuario.ADMIN) {
            return List.of(ModuloPermissao.values());
        }
        return cache.getOrDefault(usuario.getId(), List.of());
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizarBusca(String busca) {
        return busca == null || busca.isBlank() ? null : busca.trim();
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String gerarSenhaTemporaria() {
        StringBuilder senha = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            senha.append(ALFABETO_SENHA.charAt(RANDOM.nextInt(ALFABETO_SENHA.length())));
        }
        return senha.toString();
    }
}
