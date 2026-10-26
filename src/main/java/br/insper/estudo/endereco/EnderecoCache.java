package br.insper.estudo.endereco;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * Aula 09: CACHE no Redis com TTL.
 * Guarda o JSON do endereço com chave "cep:<numero>" por 1 hora.
 * Se o Redis cair, o cache apenas "some" (try/catch): a API continua funcionando, só fica mais lenta.
 * Isso é um trade-off consciente: cache é otimização, não pode virar ponto único de falha.
 */
@Component
public class EnderecoCache {

    private static final Logger log = LoggerFactory.getLogger(EnderecoCache.class);
    private static final String PREFIXO = "cep:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public EnderecoCache(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    public Optional<Endereco> obter(String cep) {
        try {
            String valor = redis.opsForValue().get(PREFIXO + cep);
            return valor == null ? Optional.empty() : Optional.of(json.readValue(valor, Endereco.class));
        } catch (Exception e) {
            log.warn("Cache indisponível ao ler: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void guardar(String cep, Endereco endereco) {
        try {
            redis.opsForValue().set(PREFIXO + cep, json.writeValueAsString(endereco), TTL);
        } catch (Exception e) {
            log.warn("Cache indisponível ao gravar: {}", e.getMessage());
        }
    }
}
