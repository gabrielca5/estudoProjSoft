package br.insper.estudo.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProcessadorBoleto implements ProcessadorPagamento {
    public TipoPagamento tipo() { return TipoPagamento.BOLETO; }

    public String processar(BigDecimal valor) {
        return "Boleto de R$ " + valor + " gerado, vence em 3 dias";
    }
}
