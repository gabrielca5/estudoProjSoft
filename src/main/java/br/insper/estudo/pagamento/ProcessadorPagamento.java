package br.insper.estudo.pagamento;

import java.math.BigDecimal;

/**
 * PADRÃO STRATEGY (Aula 02): cada forma de pagamento é uma "estratégia" com o mesmo contrato.
 * Sem isso, o service teria um if/else gigante que cresce a cada forma nova.
 */
public interface ProcessadorPagamento {
    TipoPagamento tipo();
    String processar(BigDecimal valor);
}
