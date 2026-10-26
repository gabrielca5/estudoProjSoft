package br.insper.estudo.observer;

import br.insper.estudo.entity.Auditoria;
import br.insper.estudo.entity.Avaliacao;
import br.insper.estudo.repository.AuditoriaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Cada observer testado sozinho: entra um evento, conferimos o efeito. */
class ObserversTests {

    @Test
    void auditoria_deveSalvarUmaLinhaComAOperacao() {
        AuditoriaRepository repo = Mockito.mock(AuditoriaRepository.class); // mock sem anotação
        Avaliacao a = new Avaliacao();
        a.setId(7L);

        new AuditoriaObserver(repo).atualizar(a, Operacao.DELETE);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        Mockito.verify(repo).save(captor.capture());
        Assertions.assertEquals(7L, captor.getValue().getAvaliacaoId());
        Assertions.assertEquals(Operacao.DELETE, captor.getValue().getOperacao());
        Assertions.assertNotNull(captor.getValue().getDataHora());
    }

    @Test
    void moderacao_cobreTodosOsCaminhos() {
        ModeracaoObserver m = new ModeracaoObserver();
        Avaliacao a = new Avaliacao();
        a.setNota(1);
        Assertions.assertDoesNotThrow(() -> m.atualizar(a, Operacao.CREATE)); // nota baixa: loga aviso
        Assertions.assertDoesNotThrow(() -> m.atualizar(a, Operacao.DELETE)); // delete: ignora
        a.setNota(5);
        Assertions.assertDoesNotThrow(() -> m.atualizar(a, Operacao.CREATE)); // nota alta: ignora
    }
}
