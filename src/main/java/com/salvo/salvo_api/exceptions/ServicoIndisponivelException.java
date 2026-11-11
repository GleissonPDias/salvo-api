package com.salvo.salvo_api.exceptions;

/**
 * Dependência externa necessária para a operação não está configurada ou
 * está indisponível (ex.: Supabase Auth Admin API sem service role key).
 */
public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String message) {
        super(message);
    }
}
