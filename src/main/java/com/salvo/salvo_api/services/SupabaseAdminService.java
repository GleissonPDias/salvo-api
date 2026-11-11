package com.salvo.salvo_api.services;

import com.salvo.salvo_api.exceptions.RegraDeNegocioException;
import com.salvo.salvo_api.exceptions.ServicoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Operações administrativas no Supabase Auth.
 *
 * Usa a service role key, que só pode existir no back-end. Nunca expor essa
 * chave para o front-end. Sem ela, o cadastro de funcionários fica indisponível
 * (503) e a API continua funcionando para os demais fluxos.
 */
@Service
public class SupabaseAdminService {

    private static final String BAN_DURATION = "876000h";

    private final RestClient restClient;
    private final String authUrl;
    private final String serviceRoleKey;

    public SupabaseAdminService(
            @Value("${salvo.security.supabase.auth-url:https://gkgnfxkbwexpihqybcue.supabase.co/auth/v1}") String authUrl,
            @Value("${salvo.security.supabase.service-role-key:}") String serviceRoleKey) {
        this.restClient = RestClient.create();
        this.authUrl = authUrl;
        this.serviceRoleKey = serviceRoleKey;
    }

    public UUID criarUsuario(String email, String senha, Map<String, Object> metadata) {
        exigirConfigurado();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", senha);
        body.put("email_confirm", true);
        body.put("user_metadata", metadata);

        try {
            Map<String, Object> resposta = restClient.post()
                .uri(authUrl + "/admin/users")
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            return extrairId(resposta, "criar usuário");
        } catch (RestClientResponseException ex) {
            if (emailJaCadastrado(ex)) {
                throw new RegraDeNegocioException("E-mail já cadastrado no Supabase Auth");
            }
            throw new RegraDeNegocioException(
                "Falha ao criar o usuário no Supabase (" + ex.getStatusCode() + "): "
                    + ex.getResponseBodyAsString()
            );
        } catch (RegraDeNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServicoIndisponivelException(
                "Erro ao conectar ao Supabase Auth: " + ex.getMessage()
            );
        }
    }

    /**
     * Envia convite por e-mail. O funcionário define a senha pelo link.
     * Requer SMTP e URL de redirecionamento configurados no Supabase.
     */
    public UUID convidarUsuario(String email, Map<String, Object> metadata) {
        exigirConfigurado();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("data", metadata);

        try {
            Map<String, Object> resposta = restClient.post()
                .uri(authUrl + "/invite")
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            return extrairId(resposta, "convidar usuário");
        } catch (RestClientResponseException ex) {
            if (emailJaCadastrado(ex)) {
                throw new RegraDeNegocioException("E-mail já cadastrado no Supabase Auth");
            }
            throw new RegraDeNegocioException(
                "Falha ao enviar o convite pelo Supabase (" + ex.getStatusCode() + "): "
                    + ex.getResponseBodyAsString()
            );
        } catch (RegraDeNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServicoIndisponivelException(
                "Erro ao conectar ao Supabase Auth: " + ex.getMessage()
            );
        }
    }

    public void atualizarSenha(UUID authUserId, String senha) {
        exigirConfigurado();
        atualizarUsuario(authUserId, Map.of("password", senha), "redefinir a senha");
    }

    /**
     * Banimento opcional no Supabase. A API já bloqueia usuários inativos no
     * OficinaContexto; o banimento evita que a sessão seja renovada no login.
     */
    public void definirBanimento(UUID authUserId, boolean banido) {
        if (authUserId == null || !configurado()) return;
        try {
            atualizarUsuario(
                authUserId,
                Map.of("ban_duration", banido ? BAN_DURATION : "none"),
                banido ? "banir usuário" : "desbanir usuário"
            );
        } catch (RuntimeException ignored) {
            // Best-effort: o controle principal é o campo ativo no banco.
        }
    }

    public void excluirUsuario(UUID authUserId) {
        if (authUserId == null) return;
        exigirConfigurado();
        try {
            restClient.delete()
                .uri(authUrl + "/admin/users/{id}", authUserId)
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new RegraDeNegocioException(
                "Falha ao excluir o usuário no Supabase (" + ex.getStatusCode() + "): "
                    + ex.getResponseBodyAsString()
            );
        } catch (Exception ex) {
            throw new ServicoIndisponivelException(
                "Erro ao conectar ao Supabase Auth: " + ex.getMessage()
            );
        }
    }

    public boolean configurado() {
        return serviceRoleKey != null && !serviceRoleKey.isBlank();
    }

    private void atualizarUsuario(UUID authUserId, Map<String, Object> body, String acao) {
        try {
            restClient.put()
                .uri(authUrl + "/admin/users/{id}", authUserId)
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new RegraDeNegocioException(
                "Falha ao " + acao + " no Supabase (" + ex.getStatusCode() + "): "
                    + ex.getResponseBodyAsString()
            );
        }
    }

    private void exigirConfigurado() {
        if (!configurado()) {
            throw new ServicoIndisponivelException(
                "Cadastro de funcionários indisponível: configure SUPABASE_SERVICE_ROLE_KEY no back-end."
            );
        }
    }

    private UUID extrairId(Map<String, Object> resposta, String acao) {
        if (resposta == null) {
            throw new RegraDeNegocioException("Resposta vazia do Supabase ao " + acao + ".");
        }
        Object id = resposta.get("id");
        if (id == null && resposta.get("user") instanceof Map<?, ?> usuario) {
            id = usuario.get("id");
        }
        if (id == null) {
            throw new RegraDeNegocioException(
                "O Supabase não retornou o id do usuário ao " + acao + "."
            );
        }
        try {
            return UUID.fromString(String.valueOf(id));
        } catch (IllegalArgumentException ex) {
            throw new RegraDeNegocioException(
                "O Supabase retornou um id de usuário inválido ao " + acao + "."
            );
        }
    }

    private boolean emailJaCadastrado(RestClientResponseException ex) {
        String detalhe = ex.getResponseBodyAsString();
        return ex.getStatusCode().value() == 422
            || ex.getStatusCode().value() == 409
            || (detalhe != null && (
                detalhe.toLowerCase().contains("already registered")
                    || detalhe.toLowerCase().contains("already been registered")
                    || detalhe.toLowerCase().contains("user already exists")
            ));
    }
}
