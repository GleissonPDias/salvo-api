package com.salvo.salvo_api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.salvo.salvo_api.dtos.LoginRequestDTO;
import com.salvo.salvo_api.dtos.LoginResponseDTO;
import com.salvo.salvo_api.dtos.RegistrarOficinaDTO;
import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.repositories.OficinaRepository;
import com.salvo.salvo_api.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final RestClient restClient;
    private final String authUrl;
    private final String anonKey;
    private final OficinaRepository oficinaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuthService(
            @Value("${salvo.security.supabase.auth-url:https://gkgnfxkbwexpihqybcue.supabase.co/auth/v1}") String authUrl,
            @Value("${salvo.security.supabase.anon-key:}") String anonKey,
            OficinaRepository oficinaRepository,
            UsuarioRepository usuarioRepository) {
        this.restClient = RestClient.create();
        this.authUrl = authUrl;
        this.anonKey = anonKey;
        this.oficinaRepository = oficinaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
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
                            "email", dto.email(),
                            "password", dto.password()
                    ))
                    .retrieve()
                    .body(LoginResponseDTO.class);
        } catch (RestClientResponseException ex) {
            throw new RegraDeNegocioException(
                "Falha na autenticação com Supabase (" + ex.getStatusCode() + "): " + ex.getResponseBodyAsString()
            );
        } catch (Exception ex) {
            throw new RegraDeNegocioException("Erro ao conectar ao Supabase Auth: " + ex.getMessage());
        }
    }

    @Transactional
    public LoginResponseDTO registrar(RegistrarOficinaDTO dto) {
        if (anonKey == null || anonKey.isBlank()) {
            throw new RegraDeNegocioException(
                "A chave anon-key do Supabase precisa ser configurada em salvo.security.supabase.anon-key."
            );
        }

        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new RegraDeNegocioException("O e-mail informado já está cadastrado no sistema.");
        }

        JsonNode signupResponse;
        try {
            signupResponse = restClient.post()
                    .uri(authUrl + "/signup")
                    .header("apikey", anonKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email", dto.email(),
                            "password", dto.password(),
                            "data", Map.of("nome", dto.nomeUsuario())
                    ))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw new RegraDeNegocioException(
                "Erro ao cadastrar usuário no Supabase: " + ex.getResponseBodyAsString()
            );
        } catch (Exception ex) {
            throw new RegraDeNegocioException("Erro ao conectar ao Supabase Auth: " + ex.getMessage());
        }

        if (signupResponse == null) {
            throw new RegraDeNegocioException("Não foi possível obter resposta do Supabase Auth.");
        }

        String authUserIdStr = null;
        if (signupResponse.hasNonNull("id")) {
            authUserIdStr = signupResponse.get("id").asText();
        } else if (signupResponse.hasNonNull("user") && signupResponse.get("user").hasNonNull("id")) {
            authUserIdStr = signupResponse.get("user").get("id").asText();
        }

        if (authUserIdStr == null || authUserIdStr.isBlank()) {
            throw new RegraDeNegocioException("Falha ao registrar usuário: ID de autenticação não retornado.");
        }

        UUID authUserId = UUID.fromString(authUserIdStr);

        // 1. Cria a Oficina (Tenant)
        Oficina oficina = new Oficina();
        oficina.setNome(dto.nomeOficina());
        oficina.setCnpj(dto.cnpj());
        oficina.setTelefone(dto.telefone());
        oficina.setAtiva(true);
        Oficina oficinaSalva = oficinaRepository.save(oficina);

        // 2. Cria o Usuário administrador vinculado à oficina
        Usuario usuario = new Usuario();
        usuario.setOficina(oficinaSalva);
        usuario.setAuthUserId(authUserId);
        usuario.setNome(dto.nomeUsuario());
        usuario.setEmail(dto.email());
        usuario.setCargo("ADMIN");
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);

        // 3. Tenta autenticar imediatamente para devolver os tokens de login
        try {
            return login(new LoginRequestDTO(dto.email(), dto.password()));
        } catch (Exception ex) {
            // Se o Supabase exigir confirmação de e-mail e não permitir login imediato:
            if (signupResponse.hasNonNull("access_token")) {
                return new LoginResponseDTO(
                        signupResponse.get("access_token").asText(),
                        signupResponse.hasNonNull("token_type") ? signupResponse.get("token_type").asText() : "bearer",
                        signupResponse.hasNonNull("expires_in") ? signupResponse.get("expires_in").asLong() : 3600L,
                        signupResponse.hasNonNull("refresh_token") ? signupResponse.get("refresh_token").asText() : null
                );
            }
            if (signupResponse.hasNonNull("session") && signupResponse.get("session").hasNonNull("access_token")) {
                JsonNode sessionNode = signupResponse.get("session");
                return new LoginResponseDTO(
                        sessionNode.get("access_token").asText(),
                        sessionNode.hasNonNull("token_type") ? sessionNode.get("token_type").asText() : "bearer",
                        sessionNode.hasNonNull("expires_in") ? sessionNode.get("expires_in").asLong() : 3600L,
                        sessionNode.hasNonNull("refresh_token") ? sessionNode.get("refresh_token").asText() : null
                );
            }
            return new LoginResponseDTO(null, null, null, null);
        }
    }
}
