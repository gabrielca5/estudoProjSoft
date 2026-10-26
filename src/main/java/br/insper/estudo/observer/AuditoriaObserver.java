package br.insper.estudo.observer;

import br.insper.estudo.entity.Auditoria;
import br.insper.estudo.entity.Avaliacao;
import br.insper.estudo.repository.AuditoriaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Observer 1: persiste cada ação no banco (requisito de auditoria da prova). */
@Component
public class AuditoriaObserver implements AvaliacaoObserver {

    private final AuditoriaRepository repository;

    public AuditoriaObserver(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void atualizar(Avaliacao avaliacao, Operacao operacao) {
        repository.save(new Auditoria(avaliacao.getId(), operacao, LocalDateTime.now()));
    }
}
