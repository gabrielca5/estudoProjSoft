package br.insper.estudo.pagamento;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * PADRÃO FACTORY (Aula 02): centraliza "qual objeto criar/usar para este tipo".
 * O Spring injeta todos os ProcessadorPagamento; a factory indexa por tipo.
 * Adicionar ApplePay = criar ProcessadorApplePay + valor no enum. Nada mais muda.
 */
@Component
public class PagamentoFactory {

    private final Map<TipoPagamento, ProcessadorPagamento> processadores = new EnumMap<>(TipoPagamento.class);

    public PagamentoFactory(List<ProcessadorPagamento> lista) {
        lista.forEach(p -> processadores.put(p.tipo(), p));
    }

    public ProcessadorPagamento getProcessador(TipoPagamento tipo) {
        ProcessadorPagamento p = processadores.get(tipo);
        if (p == null) {
            throw new IllegalArgumentException("Tipo de pagamento não suportado: " + tipo);
        }
        return p;
    }
}
