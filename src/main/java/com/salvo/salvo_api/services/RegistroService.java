package com.salvo.salvo_api.services;

import com.salvo.salvo_api.dtos.LoginResponseDTO;
import com.salvo.salvo_api.dtos.RegistrarOficinaDTO;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.OficinaRepository;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cria a conta SaaS: identidade no Supabase Auth + oficina/usuário no banco.
 */
@Service
@RequiredArgsConstructor
public class RegistroService {

    private static final List<String[]> CHECKLIST_PADRAO = List.of(
        new String[]{"farois", "Faróis e luz alta"},
        new String[]{"lanternas", "Lanternas e luz de freio"},
        new String[]{"setas", "Setas e pisca-alerta"},
        new String[]{"limpadores", "Limpadores e esguicho"},
        new String[]{"pneus", "Pneus (desgaste e calibragem)"},
        new String[]{"estepe", "Estepe"},
        new String[]{"ferramentas", "Macaco, chave de roda e triângulo"},
        new String[]{"documento", "Documento do veículo (CRLV)"},
        new String[]{"multimidia", "Rádio / Multimídia"},
        new String[]{"ar_condicionado", "Ar-condicionado"},
        new String[]{"vidros", "Vidros e travas elétricas"},
        new String[]{"painel", "Painel sem luzes de alerta"}
    );

    private final AuthService authService;
    private final OficinaRepository oficinaRepository;
    private final UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public LoginResponseDTO registrar(RegistrarOficinaDTO dto) {
        String email = dto.email().trim().toLowerCase();
        String cnpj = normalizarDocumento(dto.cnpj());

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraDeNegocioException("E-mail já cadastrado");
        }
        if (cnpj != null && oficinaRepository.existsByCnpj(cnpj)) {
            throw new RegraDeNegocioException("CNPJ já cadastrado");
        }

        Map<String, Object> signup = authService.signup(email, dto.password(), dto.nomeUsuario(), dto.nomeOficina());
        String authUserId = extrairAuthUserId(signup);
        if (authUserId == null) {
            throw new RegraDeNegocioException(
                "O Supabase não retornou o id do usuário criado. Verifique se o cadastro por e-mail está habilitado."
            );
        }

        Oficina oficina = new Oficina();
        oficina.setNome(dto.nomeOficina().trim());
        oficina.setNomeFantasia(dto.nomeOficina().trim());
        oficina.setEmail(email);
        oficina.setTelefone(dto.telefone());
        oficina.setCnpj(cnpj);
        oficina.setAtiva(true);
        oficina = oficinaRepository.save(oficina);

        Usuario usuario = new Usuario();
        usuario.setOficina(oficina);
        usuario.setAuthUserId(UUID.fromString(authUserId));
        usuario.setNome(dto.nomeUsuario().trim());
        usuario.setEmail(email);
        usuario.setCargo("ADMIN");
        usuario.setPerfil(PerfilUsuario.ADMIN);
        usuario.setDono(true);
        usuario.setExecutaOs(true);
        usuario.setDeveTrocarSenha(false);
        usuario.setTelefone(dto.telefone());
        usuario.setLider(true);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);

        criarChecklistPadrao(oficina.getId());

        return new LoginResponseDTO(
            texto(signup.get("access_token")),
            texto(signup.get("token_type")),
            numero(signup.get("expires_in")),
            texto(signup.get("refresh_token"))
        );
    }

    private void criarChecklistPadrao(Long oficinaId) {
        for (String[] item : CHECKLIST_PADRAO) {
            em.createNativeQuery(
                    "INSERT INTO public.checklist_itens (oficina_id, codigo, nome) " +
                    "VALUES (:oficina, :codigo, :nome) " +
                    "ON CONFLICT (oficina_id, codigo) DO NOTHING")
                .setParameter("oficina", oficinaId)
                .setParameter("codigo", item[0])
                .setParameter("nome", item[1])
                .executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    private String extrairAuthUserId(Map<String, Object> signup) {
        Object user = signup.get("user");
        if (user instanceof Map<?, ?> mapa) {
            Object id = mapa.get("id");
            if (id != null) return String.valueOf(id);
        }
        Object id = signup.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    private String normalizarDocumento(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String texto(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Long numero(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
