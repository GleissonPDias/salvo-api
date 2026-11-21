package com.salvo.salvo_api.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Envolve a resposta em um wrapper que armazena o corpo em cache, para que o
 * {@link AuditoriaInterceptor} possa ler o JSON retornado (por exemplo, o nome
 * do registro criado/editado) e enriquecer a trilha de auditoria.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditoriaResponseCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        request.setAttribute("auditoria.responseWrapper", wrapper);
        filterChain.doFilter(request, wrapper);
        wrapper.copyBodyToResponse();
    }
}
