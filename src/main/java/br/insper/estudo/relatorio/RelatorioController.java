package br.insper.estudo.relatorio;

import br.insper.estudo.exception.RecursoNaoEncontradoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * Fluxo assíncrono do slide da Aula 09:
 *   POST /relatorios        -> 202 Accepted + id (não espera o trabalho terminar)
 *   GET  /relatorios/{id}   -> cliente consulta o status (PENDENTE / PRONTO)
 */
@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioFila fila;

    public RelatorioController(RelatorioFila fila) {
        this.fila = fila;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> pedir(@RequestParam(defaultValue = "vendas") String tipo) {
        String id = fila.publicar(tipo);
        return ResponseEntity.accepted()
                .location(URI.create("/relatorios/" + id))
                .body(Map.of("id", id, "status", "PENDENTE"));
    }

    @GetMapping("/{id}")
    public Map<Object, Object> status(@PathVariable String id) {
        Map<Object, Object> estado = fila.estado(id);
        if (estado.isEmpty()) {
            throw new RecursoNaoEncontradoException("Relatório " + id + " não encontrado");
        }
        return estado;
    }
}
