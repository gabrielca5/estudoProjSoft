package br.insper.estudo.service;

import br.insper.estudo.dto.AvaliacaoRequest;
import br.insper.estudo.dto.AvaliacaoResponse;
import br.insper.estudo.entity.Avaliacao;
import br.insper.estudo.exception.RecursoNaoEncontradoException;
import br.insper.estudo.observer.AvaliacaoObserver;
import br.insper.estudo.observer.Operacao;
import br.insper.estudo.repository.AvaliacaoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

/**
 * Aula 06: TESTE DE UNIDADE do service.
 *
 * "Unidade" = só esta classe. Tudo que ela usa (repository, observers) vira MOCK:
 * um objeto falso em que NÓS dizemos o que cada método devolve (Mockito.when)
 * e depois conferimos se foi chamado (Mockito.verify). Sem banco, sem Spring, sem rede.
 *
 * Estrutura de todo teste (AAA):
 *   Arrange  -> monta dados e mocks
 *   Act      -> chama o método testado
 *   Assert   -> confere o resultado e as interações
 *
 * Para 100% de cobertura: um teste para CADA caminho (if verdadeiro, if falso, exceção).
 */
@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTests {

    @Mock
    private AvaliacaoRepository repository;

    @Mock
    private AvaliacaoObserver observer;

    private AvaliacaoService service;

    @BeforeEach
    void setUp() {
        // como o service usa injeção por construtor, criamos ele "na mão" com os mocks
        service = new AvaliacaoService(repository, List.of(observer));
    }

    /** helper: uma entidade já "salva" no banco */
    private Avaliacao entidade(Long id, String autor, int nota) {
        Avaliacao a = new Avaliacao();
        a.setId(id);
        a.setAutor(autor);
        a.setConteudo("comentário");
        a.setNota(nota);
        return a;
    }

    // ================= criar =================

    @Test
    void criar_deveSalvarPreencherDataENotificarObservers() {
        // Arrange
        AvaliacaoRequest request = new AvaliacaoRequest("gabriel", "muito bom", 5);
        // quando o service chamar save(qualquer coisa), devolve o próprio objeto com id 1
        Mockito.when(repository.save(Mockito.any(Avaliacao.class))).thenAnswer(invocacao -> {
            Avaliacao a = invocacao.getArgument(0);
            a.setId(1L);
            return a;
        });

        // Act
        AvaliacaoResponse response = service.criar(request);

        // Assert: resultado
        Assertions.assertEquals(1L, response.id());
        Assertions.assertEquals("gabriel", response.autor());
        Assertions.assertNotNull(response.dataAvaliacao());

        // Assert: o que foi salvo de fato (ArgumentCaptor "captura" o argumento passado ao mock)
        ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
        Mockito.verify(repository).save(captor.capture());
        Assertions.assertFalse(captor.getValue().isDeletado());

        // Assert: o Observer foi avisado com CREATE
        Mockito.verify(observer).atualizar(Mockito.any(Avaliacao.class), Mockito.eq(Operacao.CREATE));
    }

    @Test
    void criar_comNotaNula_deveLancarExcecaoESemSalvar() {
        AvaliacaoRequest request = new AvaliacaoRequest("gabriel", "x", null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> service.criar(request));

        // nada foi salvo nem notificado
        Mockito.verifyNoInteractions(repository, observer);
    }

    @Test
    void criar_comNotaMenorQue1_deveLancarExcecao() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.criar(new AvaliacaoRequest("a", "b", 0)));
    }

    @Test
    void criar_comNotaMaiorQue5_deveLancarExcecao() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.criar(new AvaliacaoRequest("a", "b", 6)));
    }

    // ================= listar =================

    @Test
    void listar_semFiltro_deveBuscarTodasNaoDeletadas() {
        Mockito.when(repository.findByDeletadoFalse())
                .thenReturn(List.of(entidade(1L, "ana", 4), entidade(2L, "bia", 3)));

        List<AvaliacaoResponse> lista = service.listar(null);

        Assertions.assertEquals(2, lista.size());
        Assertions.assertEquals("ana", lista.get(0).autor());
    }

    @Test
    void listar_comFiltroEmBranco_deveIgnorarFiltro() {
        Mockito.when(repository.findByDeletadoFalse()).thenReturn(List.of());

        Assertions.assertTrue(service.listar("   ").isEmpty());
        Mockito.verify(repository, Mockito.never()).findByDeletadoFalseAndAutorStartingWith(Mockito.anyString());
    }

    @Test
    void listar_comFiltro_deveUsarStartsWith() {
        Mockito.when(repository.findByDeletadoFalseAndAutorStartingWith("gab"))
                .thenReturn(List.of(entidade(1L, "gabriel", 5)));

        List<AvaliacaoResponse> lista = service.listar("gab");

        Assertions.assertEquals(1, lista.size());
    }

    // ================= buscarPorId =================

    @Test
    void buscarPorId_quandoExiste_deveRetornarDto() {
        Mockito.when(repository.findByIdAndDeletadoFalse(1L)).thenReturn(Optional.of(entidade(1L, "gabriel", 5)));

        AvaliacaoResponse r = service.buscarPorId(1L);

        Assertions.assertEquals(1L, r.id());
    }

    @Test
    void buscarPorId_quandoNaoExiste_deveLancarNaoEncontrado() {
        Mockito.when(repository.findByIdAndDeletadoFalse(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    // ================= excluir =================

    @Test
    void excluir_deveMarcarComoDeletadaSalvarENotificar() {
        Avaliacao existente = entidade(1L, "gabriel", 2);
        Mockito.when(repository.findByIdAndDeletadoFalse(1L)).thenReturn(Optional.of(existente));

        service.excluir(1L);

        Assertions.assertTrue(existente.isDeletado());
        Mockito.verify(repository).save(existente);
        Mockito.verify(observer).atualizar(existente, Operacao.DELETE);
    }

    @Test
    void excluir_quandoNaoExiste_naoDeveSalvarNemNotificar() {
        Mockito.when(repository.findByIdAndDeletadoFalse(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(RecursoNaoEncontradoException.class, () -> service.excluir(1L));

        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
        Mockito.verifyNoInteractions(observer);
    }
}
