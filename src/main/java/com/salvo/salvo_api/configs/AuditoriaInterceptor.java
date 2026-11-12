package com.salvo.salvo_api.configs;

import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.services.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

/**
 * Registra na auditoria toda operação de escrita concluída com sucesso.
 *
 * A leitura do usuário logado e a gravação são protegidas por try/catch:
 * uma falha na auditoria nunca deve derrubar a operação de negócio.
 */
@Component
@RequiredArgsConstructor
public class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaInterceptor.class);

    private static final String[] PREFIXOS_IGNORADOS = {
        "/auth", "/notificacoes", "/actuator", "/v3/api-docs", "/swagger-ui", "/error"
    };

    private final AuditoriaService auditoriaService;
    private final OficinaContexto oficinaContexto;

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        String metodo = request.getMethod();
        String acao = acao(metodo);
        if (acao == null) return;

        int status = response.getStatus();
        if (status < 200 || status >= 300) return;

        String caminho = request.getRequestURI();
        if (ignorado(caminho)) return;

        try {
            Usuario usuario = oficinaContexto.getUsuarioLogado();
            Contexto contexto = classificar(caminho);
            String entidadeId = entidadeId(caminho);

            auditoriaService.registrar(
                usuario,
                acao,
                contexto.modulo(),
                contexto.entidade(),
                entidadeId,
                metodo,
                caminho,
                status,
                resumo(acao, contexto.entidade(), entidadeId, caminho)
            );
        } catch (RuntimeException e) {
            log.warn("Falha ao registrar auditoria de {} {}: {}", metodo, caminho, e.getMessage());
        }
    }

    private boolean ignorado(String caminho) {
        for (String prefixo : PREFIXOS_IGNORADOS) {
            if (caminho.startsWith(prefixo)) return true;
        }
        return false;
    }

    private String acao(String metodo) {
        return switch (metodo.toUpperCase(Locale.ROOT)) {
            case "POST" -> "CRIAR";
            case "PUT" -> "ATUALIZAR";
            case "PATCH" -> "ALTERAR";
            case "DELETE" -> "EXCLUIR";
            default -> null;
        };
    }

    private String verbo(String acao) {
        return switch (acao) {
            case "CRIAR" -> "Criou";
            case "ATUALIZAR" -> "Atualizou";
            case "ALTERAR" -> "Alterou";
            case "EXCLUIR" -> "Excluiu";
            default -> "Alterou";
        };
    }

    private String resumo(String acao, String entidade, String entidadeId, String caminho) {
        if (entidade == null) return verbo(acao) + " " + caminho;

        return verbo(acao) + " " + entidade + (entidadeId != null ? " #" + entidadeId : "");
    }

    private String entidadeId(String caminho) {
        String[] segmentos = segmentos(caminho);
        if (segmentos.length == 0) return null;

        String ultimo = segmentos[segmentos.length - 1];
        return ultimo.matches("\\d+") ? ultimo : null;
    }

    private Contexto classificar(String caminho) {
        String[] segmentos = segmentos(caminho);
        if (segmentos.length == 0) return new Contexto(null, null);

        return switch (segmentos[0]) {
            case "clientes" -> segmentos.length >= 3 && "veiculos".equals(segmentos[2])
                ? new Contexto("VEICULOS", "Veiculo")
                : new Contexto("CLIENTES", "Cliente");
            case "veiculos" -> new Contexto("VEICULOS", "Veiculo");
            case "servicos" -> new Contexto("OS", "Servico");
            case "pecas", "estoque" -> new Contexto("ESTOQUE", "Peca");
            case "fornecedores" -> new Contexto("ESTOQUE", "Fornecedor");
            case "pedidos-compra" -> new Contexto("ESTOQUE", "PedidoCompra");
            case "ordens-servico" -> new Contexto("OS", "OrdemServico");
            case "agendamentos" -> new Contexto("AGENDA", "Agendamento");
            case "pagamentos" -> new Contexto("FINANCEIRO", "Pagamento");
            case "despesas" -> new Contexto("FINANCEIRO", "Despesa");
            case "boxes", "oficina" -> new Contexto("CONFIGURACOES", "Configuracao");
            case "usuarios" -> new Contexto("EQUIPE", "Usuario");
            default -> new Contexto(null, null);
        };
    }

    private String[] segmentos(String caminho) {
        String limpo = caminho.startsWith("/") ? caminho.substring(1) : caminho;
        if (limpo.isBlank()) return new String[0];
        return limpo.split("/");
    }

    private record Contexto(String modulo, String entidade) {}
}
