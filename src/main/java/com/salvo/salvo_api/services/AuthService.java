package com.salvo.salvo_api.services;

import com.salvo.salvo_api.dtos.LoginRequestDTO;
import com.salvo.salvo_api.dtos.LoginResponseDTO;
import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
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
}
