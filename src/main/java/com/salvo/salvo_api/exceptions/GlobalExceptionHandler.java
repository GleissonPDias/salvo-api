package com.salvo.salvo_api.exceptions;

import com.salvo.salvo_api.dtos.ErroDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
        if (causa != null && causa.contains("veiculos_placa_key")) {
            return new ErroDTO("CONFLITO", "Placa já cadastrada no sistema");
        }
        if (causa != null && causa.contains("veiculos_chassi_key")) {
            return new ErroDTO("CONFLITO", "Chassi já cadastrado no sistema");
        }
        return new ErroDTO("CONFLITO", "Dado duplicado");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroDTO handleErroGenerico(Exception ex) {
        return new ErroDTO("ERRO_INTERNO", "Erro inesperado: " + ex.getMessage());
    }
}
