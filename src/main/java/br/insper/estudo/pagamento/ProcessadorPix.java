package br.insper.estudo.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProcessadorPix implements ProcessadorPagamento {
    public TipoPagamento tipo() { return TipoPagamento.PIX; }

    public String processar(BigDecimal valor) {
        return "PIX de R$ " + valor + " enviado na hora";
    }
}
