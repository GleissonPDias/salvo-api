package com.salvo.salvo_api.configs;

import com.salvo.salvo_api.entities.Oficina;
import com.salvo.salvo_api.entities.Usuario;
import com.salvo.salvo_api.services.AuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaInterceptorTest {

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private OficinaContexto oficinaContexto;

    private AuditoriaInterceptor interceptor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        interceptor = new AuditoriaInterceptor(auditoriaService, oficinaContexto);

        Oficina oficina = new Oficina();
        oficina.setId(16L);

        usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNome("Carlos");
        usuario.setOficina(oficina);
    }

    @Test
    void registraCriacaoDeCliente() {
        when(oficinaContexto.getUsuarioLogado()).thenReturn(usuario);
        MockHttpServletRequest request = request("POST", "/clientes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(auditoriaService).registrar(
            usuario, "CRIAR", "CLIENTES", "Cliente", null,
            "POST", "/clientes", 201, "Criou Cliente"
        );
    }

    @Test
    void registraExclusaoComIdDoRegistro() {
        when(oficinaContexto.getUsuarioLogado()).thenReturn(usuario);
        MockHttpServletRequest request = request("DELETE", "/ordens-servico/9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(204);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(auditoriaService).registrar(
            usuario, "EXCLUIR", "OS", "OrdemServico", "9",
            "DELETE", "/ordens-servico/9", 204, "Excluiu OrdemServico #9"
        );
    }

    @Test
    void naoRegistraLeitura() {
        MockHttpServletRequest request = request("GET", "/clientes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(auditoriaService, never()).registrar(
            any(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyInt(), anyString()
        );
    }

    @Test
    void naoRegistraRequisicaoComErro() {
        MockHttpServletRequest request = request("POST", "/clientes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(422);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(auditoriaService, never()).registrar(
            any(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyInt(), anyString()
        );
    }

    @Test
    void naoRegistraLogin() {
        MockHttpServletRequest request = request("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(auditoriaService, never()).registrar(
            any(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyInt(), anyString()
        );
    }

    @Test
    void falhaNaAuditoriaNaoPropagaExcecao() {
        when(oficinaContexto.getUsuarioLogado()).thenReturn(usuario);
        MockHttpServletRequest request = request("POST", "/clientes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        org.mockito.Mockito.doThrow(new RuntimeException("banco fora"))
            .when(auditoriaService)
            .registrar(any(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyString());

        interceptor.afterCompletion(request, response, new Object(), null);
    }

    private MockHttpServletRequest request(String metodo, String caminho) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, caminho);
        request.setRequestURI(caminho);
        return request;
    }
}
