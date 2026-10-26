package br.insper.estudo.controller;

import br.insper.estudo.config.SecurityConfig;
import br.insper.estudo.dto.AvaliacaoResponse;
import br.insper.estudo.exception.RecursoNaoEncontradoException;
import br.insper.estudo.service.AvaliacaoService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Meio-termo entre unidade e integração: teste de "fatia web" (@WebMvcTest).
 * Sobe SÓ a camada web (controller + tratamento de erro + validação), com o SERVICE MOCKADO.
 * Não precisa de banco nem Docker. Serve para testar rotas, status HTTP, JSON e @Valid.
 */
@WebMvcTest(AvaliacaoController.class)
@Import(SecurityConfig.class) // usa nossa config de segurança (profile padrão = tudo liberado)
class AvaliacaoControllerWebTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // coloca um mock do service no lugar do bean real
    private AvaliacaoService service;

    @Test
    void get_deveRetornarJsonDaLista() throws Exception {
        Mockito.when(service.listar(null)).thenReturn(List.of(
                new AvaliacaoResponse(1L, "ana", "ok", 4, LocalDateTime.now())));

        mockMvc.perform(get("/avaliacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].autor").value("ana"));
    }

    @Test
    void post_semAutor_deveSerBarradoPeloValidAntesDoService() throws Exception {
        mockMvc.perform(post("/avaliacoes")
                        .contentType("application/json")
                        .content("{\"conteudo\":\"y\",\"nota\":3}"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(service); // nem chegou no service
    }

    @Test
    void excecaoNaoEncontrado_viraStatus404() throws Exception {
        Mockito.doThrow(new RecursoNaoEncontradoException("não existe")).when(service).excluir(5L);

        mockMvc.perform(delete("/avaliacoes/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("não existe"));
    }
}
