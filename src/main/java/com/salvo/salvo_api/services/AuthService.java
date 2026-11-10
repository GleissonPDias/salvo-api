package com.salvo.salvo_api.services;

import com.salvo.salvo_api.dtos.LoginRequestDTO;
import com.salvo.salvo_api.dtos.LoginResponseDTO;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class AuthService {

    private final RestClient restClient;
    private final String authUrl;
    private final String anonKey;

    public AuthService(
            @Value("${salvo.security.supabase.auth-url:https://gkgnfxkbwexpihqybcue.supabase.co/auth/v1}") String authUrl,
            @Value("${salvo.security.supabase.anon-key:}") String anonKey) {
        this.restClient = RestClient.create();
        this.authUrl = authUrl;
        this.anonKey = anonKey;
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

    /**
     * Cria a identidade no Supabase Auth.
     *
     * A resposta varia conforme a confirmação de e-mail estar habilitada:
     * com sessão, vem access_token/user; sem sessão, vem apenas o usuário.
     */
    public Map<String, Object> signup(String email, String password, String nomeUsuario, String nomeOficina) {
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
