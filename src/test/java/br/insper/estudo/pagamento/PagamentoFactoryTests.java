package br.insper.estudo.pagamento;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

class PagamentoFactoryTests {

    private final PagamentoFactory factory = new PagamentoFactory(
            List.of(new ProcessadorPix(), new ProcessadorCartao(), new ProcessadorBoleto()));

    @Test
    void deveEscolherAEstrategiaPeloTipo() {
        Assertions.assertInstanceOf(ProcessadorPix.class, factory.getProcessador(TipoPagamento.PIX));
        Assertions.assertTrue(factory.getProcessador(TipoPagamento.BOLETO)
                .processar(BigDecimal.TEN).startsWith("Boleto"));
    }

    @Test
    void tipoSemImplementacao_deveFalhar() {
        PagamentoFactory soPix = new PagamentoFactory(List.of(new ProcessadorPix()));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> soPix.getProcessador(TipoPagamento.CARTAO));
    }
}
