package br.insper.estudo.observer;

import br.insper.estudo.entity.Avaliacao;

/**
 * PADRÃO OBSERVER: contrato de quem quer ser avisado quando algo acontece com uma Avaliação.
 * Cada implementação é um "observador" independente.
 */
public interface AvaliacaoObserver {
    void atualizar(Avaliacao avaliacao, Operacao operacao);
}
