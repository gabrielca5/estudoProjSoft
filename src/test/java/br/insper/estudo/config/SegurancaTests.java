package br.insper.estudo.config;

import br.insper.estudo.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aula 08: testa AUTENTICAÇÃO (401) e AUTORIZAÇÃO (403) com o profile "auth".
 * jwt() cria um token falso já "validado", então não precisa do Auth0 de verdade.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("auth")
class SegurancaTests extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getEhPublico() throws Exception {
        mockMvc.perform(get("/avaliacoes")).andExpect(status().isOk());
    }

    @Test
    void semToken_deveRetornar401() throws Exception {
        mockMvc.perform(post("/avaliacoes").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void comTokenSemRoleAdmin_deleteDeveRetornar403() throws Exception {
        mockMvc.perform(delete("/avaliacoes/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void comRoleAdmin_passaDaSeguranca() throws Exception {
        // passou pela segurança; 404 porque o id não existe (prova que chegou no controller)
        mockMvc.perform(delete("/avaliacoes/424242")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
