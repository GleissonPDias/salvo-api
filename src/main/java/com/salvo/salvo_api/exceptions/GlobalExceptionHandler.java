package com.salvo.salvo_api.exceptions;

import com.salvo.salvo_api.dtos.ErroDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroDTO handleNotFound(RecursoNaoEncontradoException ex) {
        return new ErroDTO("NAO_ENCONTRADO", ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroDTO handleAcessoNegado(AcessoNegadoException ex) {
        return new ErroDTO("ACESSO_NEGADO", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroDTO handleSpringAccessDenied(AccessDeniedException ex) {
        return new ErroDTO("ACESSO_NEGADO", "Você não tem permissão para acessar este recurso.");
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroDTO handleServicoIndisponivel(ServicoIndisponivelException ex) {
        return new ErroDTO("SERVICO_INDISPONIVEL", ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroDTO handleRegraDeNegocio(RegraDeNegocioException ex) {
        return new ErroDTO("REGRA_DE_NEGOCIO", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroDTO handleValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .toList();
        return new ErroDTO("VALIDACAO", "Dados inválidos", detalhes);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroDTO handleConflito(DataIntegrityViolationException ex) {
        String causa = ex.getMostSpecificCause().getMessage();
        if (causa != null && causa.contains("ux_clientes_oficina_cpf_cnpj")) {
            return new ErroDTO("CONFLITO", "CPF/CNPJ já cadastrado nesta oficina");
        }
        if (causa != null && (causa.contains("veiculos_placa_key")
                || causa.contains("ux_veiculos_oficina_placa"))) {
            return new ErroDTO("CONFLITO", "Placa já cadastrada nesta oficina");
        }
        if (causa != null && (causa.contains("veiculos_chassi_key")
                || causa.contains("ux_veiculos_oficina_chassi"))) {
            return new ErroDTO("CONFLITO", "Chassi já cadastrado nesta oficina");
        }
        if (causa != null && causa.contains("ux_usuarios_email")) {
            return new ErroDTO("CONFLITO", "E-mail já cadastrado");
        }
        if (causa != null && causa.contains("ux_pecas_oficina_codigo")) {
            return new ErroDTO("CONFLITO", "Código de peça já cadastrado nesta oficina");
        }
        if (causa != null && causa.contains("ux_servicos_oficina_codigo")) {
            return new ErroDTO("CONFLITO", "Código de serviço já cadastrado nesta oficina");
        }
        if (causa != null && causa.contains("ux_boxes_oficina_nome")) {
            return new ErroDTO("CONFLITO", "Box já cadastrado nesta oficina");
        }
        if (causa != null && causa.contains("ux_fornecedores_oficina_nome")) {
            return new ErroDTO("CONFLITO", "Fornecedor com este nome já cadastrado nesta oficina");
        }
        if (causa != null && causa.contains("ux_fornecedores_oficina_cnpj")) {
            return new ErroDTO("CONFLITO", "CNPJ de fornecedor já cadastrado nesta oficina");
        }
        if (causa != null && causa.contains("ux_pedidos_compra_oficina_numero")) {
            return new ErroDTO("CONFLITO", "Número de pedido de compra já usado nesta oficina");
        }
        if (causa != null && causa.contains("ux_os_vistorias_os")) {
            return new ErroDTO("CONFLITO", "Esta OS já possui uma vistoria registrada");
        }
        if (causa != null && causa.contains("ux_os_vistoria_danos_area_tipo")) {
            return new ErroDTO("CONFLITO", "Dano já registrado para esta área e tipo de avaria");
        }
        return new ErroDTO("CONFLITO", "Dado duplicado");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroDTO handleErroGenerico(Exception ex) {
        return new ErroDTO("ERRO_INTERNO", "Erro inesperado: " + ex.getMessage());
    }
}
