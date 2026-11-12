package com.salvo.salvo_api.services;

import com.salvo.salvo_api.dtos.LoginRequestDTO;
import com.salvo.salvo_api.dtos.LoginResponseDTO;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final RestClient restClient;
    private final String authUrl;
    private final String anonKey;
    private final UsuarioRepository usuarioRepository;

    public AuthService(
            @Value("${salvo.security.supabase.auth-url:https://gkgnfxkbwexpihqybcue.supabase.co/auth/v1}") String authUrl,
            @Value("${salvo.security.supabase.anon-key:}") String anonKey,
            UsuarioRepository usuarioRepository) {
        this.restClient = RestClient.create();
        this.authUrl = authUrl;
        this.anonKey = anonKey;
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        LoginResponseDTO response = autenticar(dto.email(), dto.password());

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(dto.email().trim().toLowerCase())
            .orElseThrow(() -> new RegraDeNegocioException(
                "Usuário não registrado em nenhuma oficina."
            ));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new RegraDeNegocioException("Usuário inativo. Procure o administrador da oficina.");
        }
        if (!Boolean.TRUE.equals(usuario.getDono())) {
            throw new RegraDeNegocioException(
                "Apenas o dono da oficina pode fazer login direto no app. Use a troca de usuário."
            );
        }
        return response;
    }

    /**
     * Valida as credenciais no Supabase e devolve o token do usuário.
     * Usado na troca de usuário e na validação de senha do admin.
     */
    public LoginResponseDTO autenticar(String email, String senha) {
        if (anonKey == null || anonKey.isBlank()) {
            throw new RegraDeNegocioException(
                "A chave anon-key do Supabase precisa ser configurada em salvo.security.supabase.anon-key em application-local.properties."
            );
        }

        try {
            return restClient.post()
                    .uri(authUrl + "/token?grant_type=password")
                    .header("apikey", anonKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email", email.trim().toLowerCase(),
                            "password", senha
                    ))
                    .retrieve()
                    .body(LoginResponseDTO.class);
        } catch (RestClientResponseException ex) {
            throw new RegraDeNegocioException(
                "Credenciais inválidas (" + ex.getStatusCode() + ")."
            );
        } catch (Exception ex) {
            throw new RegraDeNegocioException("Erro ao conectar ao Supabase Auth: " + ex.getMessage());
        }
    }

    public Map<String, Object> signup(String email, String password, String nomeUsuario, String nomeOficina) {
        // unchanged
        if (anonKey == null || anonKey.isBlank()) {
            throw new RegraDeNegocioException(
                "A chave anon-key do Supabase precisa ser configurada em salvo.security.supabase.anon-key."
            );
        }

        try {
            Map<String, Object> body = restClient.post()
                    .uri(authUrl + "/signup")
                    .header("apikey", anonKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email", email,
                            "password", password,
                            "data", Map.of(
                                    "nome", nomeUsuario,
                                    "oficina", nomeOficina
                            )
                    ))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (body == null) {
                throw new RegraDeNegocioException("Resposta vazia do Supabase ao criar a conta.");
            }
            return body;
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 422
                    || detail.toLowerCase().contains("already registered")
                    || detail.toLowerCase().contains("already been registered")) {
                throw new RegraDeNegocioException("E-mail já cadastrado no Supabase Auth");
            }
            throw new RegraDeNegocioException(
                "Falha ao criar a conta no Supabase (" + ex.getStatusCode() + "): " + detail
            );
        } catch (RegraDeNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RegraDeNegocioException("Erro ao conectar ao Supabase Auth: " + ex.getMessage());
        }
    }
}
