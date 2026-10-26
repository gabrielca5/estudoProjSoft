package br.insper.estudo.controller;

import br.insper.estudo.IntegracaoBase;
import br.insper.estudo.dto.AvaliacaoRequest;
import br.insper.estudo.dto.AvaliacaoResponse;
import br.insper.estudo.repository.AuditoriaRepository;
import br.insper.estudo.repository.AvaliacaoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Aula 06: TESTE DE INTEGRAÇÃO (o que valeu 2 pontos na prova).
 *
 * Sobe a aplicação INTEIRA com um Postgres real (Testcontainers, ver IntegracaoBase)
 * e faz requisições HTTP simuladas com MockMvc:
 *     MockMvc -> Controller -> Service -> Repository -> Postgres
 * Não tem mock: testa se as camadas funcionam JUNTAS.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AvaliacaoControllerTests extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper json; // converte objeto <-> JSON (Jackson)

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    /** helper: faz um POST de verdade e devolve o DTO de resposta */
    private AvaliacaoResponse criarViaApi(String autor, int nota) throws Exception {
        AvaliacaoRequest request = new AvaliacaoRequest(autor, "comentário", nota);

        MvcResult resultado = mockMvc.perform(post("/avaliacoes")
                        .contentType("application/json")
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return json.readValue(resultado.getResponse().getContentAsString(), AvaliacaoResponse.class);
    }

    @Test
    void post_deveCriarGravarNoBancoERegistrarAuditoria() throws Exception {
        long auditoriasAntes = auditoriaRepository.count();

        AvaliacaoResponse criada = criarViaApi("maria", 2);

        Assertions.assertNotNull(criada.id());
        Assertions.assertNotNull(criada.dataAvaliacao());
        Assertions.assertTrue(avaliacaoRepository.findById(criada.id()).isPresent()); // está no banco
        Assertions.assertEquals(auditoriasAntes + 1, auditoriaRepository.count());   // observer gravou
    }

    @Test
    void post_comNotaInvalida_deveRetornar400() throws Exception {
        mockMvc.perform(post("/avaliacoes")
                        .contentType("application/json")
                        .content("{\"autor\":\"x\",\"conteudo\":\"y\",\"nota\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void get_deveFiltrarPorInicioDoNomeDoAutor() throws Exception {
        criarViaApi("zeca", 4);
        criarViaApi("zelia", 5);
        criarViaApi("ana", 3);

        mockMvc.perform(get("/avaliacoes").param("autor", "ze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].deletado").doesNotExist()); // DTO esconde o campo interno
    }

    @Test
    void getPorId_deveRetornarAAvaliacao() throws Exception {
        AvaliacaoResponse criada = criarViaApi("lucas", 5);

        mockMvc.perform(get("/avaliacoes/" + criada.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autor").value("lucas"));
    }

    @Test
    void delete_deveFazerDelecaoLogica() throws Exception {
        AvaliacaoResponse criada = criarViaApi("joao", 1);

        mockMvc.perform(delete("/avaliacoes/" + criada.id()))
                .andExpect(status().isNoContent());

        // some da API...
        mockMvc.perform(get("/avaliacoes/" + criada.id()))
                .andExpect(status().isNotFound());
        // ...mas a linha continua no banco, marcada como deletada
        Assertions.assertTrue(avaliacaoRepository.findById(criada.id()).orElseThrow().isDeletado());
    }

    @Test
    void getPorId_inexistente_deveRetornar404() throws Exception {
        mockMvc.perform(get("/avaliacoes/999999"))
                .andExpect(status().isNotFound());
    }
}
