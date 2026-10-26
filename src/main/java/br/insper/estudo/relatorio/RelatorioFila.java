package br.insper.estudo.relatorio;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Aulas 07 e 09: COMUNICAÇÃO ASSÍNCRONA com fila no Redis.
 *   - a fila é uma LISTA: rightPush coloca no fim, leftPop tira do começo (FIFO)
 *   - o estado de cada pedido fica num HASH "relatorio:<id>"
 * Mesma ideia do PartidaProducer (Java) + worker.py (Python) do professor.
 */
@Component
public class RelatorioFila {

    static final String FILA = "fila:relatorios";
    static final String PREFIXO = "relatorio:";

    private final StringRedisTemplate redis;

    public RelatorioFila(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Produtor: registra o pedido como PENDENTE e publica o id na fila. */
    public String publicar(String tipo) {
        String id = UUID.randomUUID().toString();
        redis.opsForHash().putAll(PREFIXO + id, Map.of("status", "PENDENTE", "tipo", tipo));
        redis.opsForList().rightPush(FILA, id);
        return id;
    }

    public String proximo() {
        return redis.opsForList().leftPop(FILA);
    }

    public Map<Object, Object> estado(String id) {
        return redis.opsForHash().entries(PREFIXO + id);
    }

    public void concluir(String id, String resultado) {
        redis.opsForHash().putAll(PREFIXO + id, Map.of("status", "PRONTO", "resultado", resultado));
    }
}
