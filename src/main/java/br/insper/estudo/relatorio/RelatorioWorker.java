package br.insper.estudo.relatorio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consumidor da fila. Em produção seria outro processo/container (como o worker.py do professor);
 * aqui fica no mesmo app para simplificar. Liga com fila.worker.enabled=true.
 *
 * Cuidado clássico de fila: a MESMA mensagem pode ser processada 2 vezes (retry, worker reiniciando).
 * Por isso o processamento é IDEMPOTENTE: se já está PRONTO, não refaz.
 */
@Component
@ConditionalOnProperty(name = "fila.worker.enabled", havingValue = "true")
public class RelatorioWorker {

    private static final Logger log = LoggerFactory.getLogger(RelatorioWorker.class);

    private final RelatorioFila fila;

    public RelatorioWorker(RelatorioFila fila) {
        this.fila = fila;
    }

    @Scheduled(fixedDelay = 1000)
    public void consumir() throws InterruptedException {
        String id;
        try {
            id = fila.proximo();
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis fora do ar, worker parado: {}", e.getMessage());
            return;
        }
        if (id == null) {
            return; // fila vazia
        }
        if ("PRONTO".equals(fila.estado(id).get("status"))) {
            log.info("Relatório {} já processado, ignorando (idempotência)", id);
            return;
        }
        log.info("Processando relatório {}", id);
        Thread.sleep(3000); // simula trabalho demorado (gerar PDF, consultar muita coisa...)
        fila.concluir(id, "Relatório gerado em /arquivos/" + id + ".pdf");
        log.info("Relatório {} pronto", id);
    }
}
