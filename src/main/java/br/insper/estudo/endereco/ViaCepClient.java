package br.insper.estudo.endereco;

import br.insper.estudo.exception.RecursoNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Aula 07: COMUNICAÇÃO SÍNCRONA com outro serviço (REST).
 * Igual ao TimeService do repositório "partida", mas com TIMEOUT (Aula 01/04: confiabilidade).
 * Sem timeout, se o serviço externo travar, sua API trava junto.
 */
@Component
public class ViaCepClient {

    private final RestClient restClient;

    public ViaCepClient(@Value("${viacep.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Endereco buscar(String cep) {
        try {
            Endereco e = restClient.get()
                    .uri("/ws/{cep}/json/", cep)
                    .retrieve()
                    .body(Endereco.class);
            // ViaCEP responde 200 com {"erro": true} quando o CEP não existe
            if (e == null || e.cep() == null) {
                throw new RecursoNaoEncontradoException("CEP " + cep + " não encontrado");
            }
            return e;
        } catch (RestClientException ex) {
            // falha de rede, timeout, 4xx/5xx do outro serviço
            throw new ServicoExternoIndisponivelException("ViaCEP indisponível: " + ex.getMessage());
        }
    }
}
