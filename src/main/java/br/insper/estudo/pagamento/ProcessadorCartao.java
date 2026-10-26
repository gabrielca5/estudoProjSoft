package br.insper.estudo.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProcessadorCartao implements ProcessadorPagamento {
    public TipoPagamento tipo() { return TipoPagamento.CARTAO; }

    public String processar(BigDecimal valor) {
        return "Cartão autorizado: R$ " + valor;
    }
}
