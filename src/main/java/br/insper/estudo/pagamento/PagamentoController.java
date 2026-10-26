package br.insper.estudo.pagamento;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** POST /pagamentos {"tipo":"PIX","valor":100} -> usa a estratégia certa via factory. */
@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final PagamentoFactory factory;

    public PagamentoController(PagamentoFactory factory) {
        this.factory = factory;
    }

    public record PagamentoRequest(TipoPagamento tipo, BigDecimal valor) {}

    @PostMapping
    public Map<String, String> pagar(@RequestBody PagamentoRequest req) {
        String resultado = factory.getProcessador(req.tipo()).processar(req.valor());
        return Map.of("resultado", resultado);
    }
}
