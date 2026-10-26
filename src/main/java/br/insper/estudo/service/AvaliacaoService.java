package br.insper.estudo.service;

import br.insper.estudo.dto.AvaliacaoRequest;
import br.insper.estudo.dto.AvaliacaoResponse;
import br.insper.estudo.entity.Avaliacao;
import br.insper.estudo.exception.RecursoNaoEncontradoException;
import br.insper.estudo.observer.AvaliacaoObserver;
import br.insper.estudo.observer.Operacao;
import br.insper.estudo.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CAMADA SERVICE: as REGRAS DE NEGÓCIO moram aqui.
 *   - não sabe nada de HTTP (isso é do controller)
 *   - não escreve SQL (isso é do repository)
 * É a classe que a prova pede com 100% de cobertura de testes.
 *
 * PADRÃO OBSERVER (Aula 02): o service é o "sujeito". Depois de criar/excluir ele só AVISA
 * todos os observers; não sabe o que cada um faz. Comportamento novo = novo @Component
 * implementando AvaliacaoObserver, sem mexer aqui (Open/Closed).
 */
@Service
public class AvaliacaoService {

    private final AvaliacaoRepository repository;
    private final List<AvaliacaoObserver> observers;

    // Injeção por CONSTRUTOR: o Spring passa o repository e a lista com TODOS os observers.
    // No teste de unidade é só chamar new AvaliacaoService(mockRepo, List.of(mockObserver)).
    public AvaliacaoService(AvaliacaoRepository repository, List<AvaliacaoObserver> observers) {
        this.repository = repository;
        this.observers = observers;
    }

    public AvaliacaoResponse criar(AvaliacaoRequest request) {
        // regra de negócio (o @Valid do controller já barra isso, mas o service não confia em quem chama)
        if (request.nota() == null || request.nota() < 1 || request.nota() > 5) {
            throw new IllegalArgumentException("nota deve estar entre 1 e 5");
        }
        Avaliacao avaliacao = request.toEntity();
        avaliacao.setDataAvaliacao(LocalDateTime.now());

        Avaliacao salva = repository.save(avaliacao);
        notificar(salva, Operacao.CREATE);
        return AvaliacaoResponse.from(salva);
    }

    public List<AvaliacaoResponse> listar(String autor) {
        List<Avaliacao> avaliacoes;
        if (autor == null || autor.isBlank()) {
            avaliacoes = repository.findByDeletadoFalse();
        } else {
            avaliacoes = repository.findByDeletadoFalseAndAutorStartingWith(autor);
        }
        return avaliacoes.stream().map(AvaliacaoResponse::from).toList();
    }

    public AvaliacaoResponse buscarPorId(Long id) {
        return AvaliacaoResponse.from(buscarEntidade(id));
    }

    /** Deleção lógica: marca como deletada em vez de apagar a linha. */
    public void excluir(Long id) {
        Avaliacao avaliacao = buscarEntidade(id);
        avaliacao.setDeletado(true);
        repository.save(avaliacao);
        notificar(avaliacao, Operacao.DELETE);
    }

    private Avaliacao buscarEntidade(Long id) {
        return repository.findByIdAndDeletadoFalse(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação " + id + " não encontrada"));
    }

    private void notificar(Avaliacao avaliacao, Operacao operacao) {
        for (AvaliacaoObserver observer : observers) {
            observer.atualizar(avaliacao, operacao);
        }
    }
}
