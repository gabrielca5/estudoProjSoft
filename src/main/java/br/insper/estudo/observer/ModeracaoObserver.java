package br.insper.estudo.observer;

import br.insper.estudo.entity.Avaliacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Observer 2: moderação automática. Nota 1 ou 2 na criação gera um aviso no log. */
@Component
public class ModeracaoObserver implements AvaliacaoObserver {

    private static final Logger log = LoggerFactory.getLogger(ModeracaoObserver.class);

    @Override
    public void atualizar(Avaliacao avaliacao, Operacao operacao) {
        if (operacao == Operacao.CREATE && avaliacao.getNota() <= 2) {
            log.warn("MODERAÇÃO: avaliação negativa criada (id={}, nota={}, autor={})",
                    avaliacao.getId(), avaliacao.getNota(), avaliacao.getAutor());
        }
    }
}
