# Guia 1: arquitetura de um projeto Spring "normal"

Este é o guia para quando você abre um projeto vazio e não sabe por onde começar. Tudo aqui aponta para código real deste repositório, então leia com o código aberto do lado.

## A ideia em uma frase

Cada camada tem **uma** responsabilidade e só conversa com a camada vizinha:

```
         HTTP (JSON)
              │
     ┌────────▼─────────┐
     │   Controller     │  recebe a requisição, valida a entrada, devolve status HTTP
     └────────┬─────────┘
              │ DTO (AvaliacaoRequest / AvaliacaoResponse)
     ┌────────▼─────────┐
     │     Service      │  regras de negócio
     └────────┬─────────┘
              │ Entity (Avaliacao)
     ┌────────▼─────────┐
     │   Repository     │  conversa com o banco
     └────────┬─────────┘
              │ SQL (gerado pelo JPA)
          ┌───▼────┐
          │Postgres│
          └────────┘
```

Por que separar? Por causa da **manutenibilidade** (Aula 02). Se a regra "nota entre 1 e 5" estiver no service, você sabe onde procurar, muda num lugar só e testa sem subir servidor nem banco.

## Os pacotes deste projeto

```
br.insper.estudo
├── controller/   AvaliacaoController          → rotas HTTP
├── dto/          AvaliacaoRequest/Response    → formato do JSON que entra e sai
├── service/      AvaliacaoService             → regras de negócio
├── repository/   AvaliacaoRepository          → acesso ao banco
├── entity/       Avaliacao, Auditoria         → tabelas
├── observer/     ...                          → padrão de projeto (Aula 02)
├── exception/    RecursoNaoEncontrado, ErroHandler → erros viram status HTTP
└── config/       SecurityConfig               → configuração (Aula 08)
```

É o mesmo jeito que o professor organiza o repositório `pagamento` (controller/, service/, repository/, dto/, entity/, exception/). Na prova, siga esse padrão: o corretor encontra tudo rápido.

> Os pacotes `pagamento/`, `endereco/` e `relatorio/` estão organizados **por funcionalidade** (tudo da feature junto). É outro estilo válido, usado aqui para separar os exemplos das Aulas 02, 07 e 09. Para a prova, prefira por camada.

---

## Camada por camada

### 1. Entity: a tabela

`entity/Avaliacao.java`

```java
@Entity                     // vira uma tabela "avaliacao"
@Getter @Setter @NoArgsConstructor   // Lombok gera getters, setters e construtor vazio
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // id auto incremento do banco
    private Long id;
    private String autor;
    private Integer nota;
    private LocalDateTime dataAvaliacao;
    private boolean deletado = false;   // deleção lógica
}
```

- Cada atributo vira uma coluna. Com `spring.jpa.hibernate.ddl-auto=update`, o Hibernate cria a tabela sozinho.
- O JPA **exige** um construtor vazio (por isso o `@NoArgsConstructor`).
- A entity é o modelo **interno**. Ela tem campos que o cliente não deveria ver nem mandar (`deletado`).

### 2. Repository: o banco

`repository/AvaliacaoRepository.java`

```java
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findByDeletadoFalse();
    List<Avaliacao> findByDeletadoFalseAndAutorStartingWith(String prefixo);
    Optional<Avaliacao> findByIdAndDeletadoFalse(Long id);
}
```

- É uma **interface**. Você não implementa nada: o Spring Data cria a implementação.
- `JpaRepository<Entidade, TipoDoId>` já dá `save`, `findById`, `findAll`, `deleteById`, `existsById`, `count`.
- Métodos extras são "query pelo nome". Cada pedaço do nome vira SQL:

| Nome do método | SQL gerado (simplificado) |
|---|---|
| `findByAutor(String a)` | `WHERE autor = ?` |
| `findByAutorStartingWith(String p)` | `WHERE autor LIKE 'p%'` |
| `findByNotaGreaterThan(int n)` | `WHERE nota > ?` |
| `findByDeletadoFalse()` | `WHERE deletado = false` |
| `findByIdAndDeletadoFalse(Long id)` | `WHERE id = ? AND deletado = false` |
| `countByNota(int n)` | `SELECT count(*) WHERE nota = ?` |

- `findById` devolve `Optional<T>`: pode ter ou não ter valor. Você decide o que fazer se não tiver (normalmente lançar exceção que vira 404).

### 3. DTO: o contrato da API

`dto/AvaliacaoRequest.java` (o que **entra**) e `dto/AvaliacaoResponse.java` (o que **sai**).

```java
public record AvaliacaoRequest(
        @NotBlank String autor,
        @NotBlank String conteudo,
        @NotNull @Min(1) @Max(5) Integer nota
) {
    public Avaliacao toEntity() { ... }        // DTO → Entity
}

public record AvaliacaoResponse(Long id, String autor, String conteudo,
                                Integer nota, LocalDateTime dataAvaliacao) {
    public static AvaliacaoResponse from(Avaliacao a) { ... }   // Entity → DTO
}
```

Por que não usar a entity direto no controller?

1. **Segurança da entrada**: se o controller recebesse `Avaliacao`, o cliente poderia mandar `"id": 5` ou `"deletado": true` no JSON.
2. **Validação**: as anotações `@NotBlank`, `@Min` ficam no DTO. Com `@Valid` no controller, o Spring devolve **400** sozinho antes de chegar no service.
3. **Esconder detalhes internos**: o `AvaliacaoResponse` não tem `deletado`.
4. **Desacoplar**: dá para mudar a tabela sem quebrar quem consome a API.

`record` é uma classe imutável em que o Java gera construtor, getters (`autor()`, sem "get"), `equals` e `toString`. Perfeito para DTO.

Onde fazer a conversão? Aqui ficou no próprio DTO (`toEntity` e `from`). Também é comum ter uma classe `AvaliacaoMapper`. O importante é **não** espalhar `new Avaliacao(); setX(); setY()` pelo código todo.

> Em prova com pouco tempo, usar a entity direto no controller funciona (o professor faz isso em alguns repositórios). Mas saiba explicar o trade-off: é mais rápido, porém expõe o modelo interno.

### 4. Service: as regras de negócio

`service/AvaliacaoService.java`

```java
@Service
public class AvaliacaoService {
    private final AvaliacaoRepository repository;
    private final List<AvaliacaoObserver> observers;

    public AvaliacaoService(AvaliacaoRepository repository, List<AvaliacaoObserver> observers) {
        this.repository = repository;
        this.observers = observers;
    }

    public AvaliacaoResponse criar(AvaliacaoRequest request) {
        if (request.nota() == null || request.nota() < 1 || request.nota() > 5) {
            throw new IllegalArgumentException("nota deve estar entre 1 e 5");
        }
        Avaliacao avaliacao = request.toEntity();
        avaliacao.setDataAvaliacao(LocalDateTime.now());
        Avaliacao salva = repository.save(avaliacao);
        notificar(salva, Operacao.CREATE);
        return AvaliacaoResponse.from(salva);
    }
    ...
}
```

O que vai no service:
- validações de **negócio** (nota válida, saldo suficiente, horário disponível...)
- orquestração: buscar, alterar, salvar, chamar outro serviço, notificar
- conversão Entity ↔ DTO
- lançar exceções quando algo dá errado (`RecursoNaoEncontradoException`)

O que **não** vai no service: `HttpStatus`, `ResponseEntity`, `@RequestBody`. O service não sabe que existe HTTP. Isso deixa ele fácil de testar com teste de unidade.

#### Injeção de dependência

O service **não** faz `new AvaliacaoRepository()`. Ele **pede** o repository e o Spring entrega (injeção de dependência). Duas formas:

```java
// 1. por CONSTRUTOR (recomendado, usado neste projeto)
private final AvaliacaoRepository repository;
public AvaliacaoService(AvaliacaoRepository repository) { this.repository = repository; }

// 2. por CAMPO (estilo do professor; funciona igual)
@Autowired
private AvaliacaoRepository repository;
```

Construtor é melhor para testar: você cria o service na mão passando mocks (`new AvaliacaoService(mockRepo, List.of(mockObserver))`). Com `@Autowired` em campo, você usa `@InjectMocks` no teste (o `EnderecoServiceTests` mostra esse jeito).

### 5. Controller: a porta HTTP

`controller/AvaliacaoController.java`

```java
@RestController                 // toda resposta vira JSON
@RequestMapping("/avaliacoes")  // prefixo de todas as rotas
public class AvaliacaoController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)                       // 201
    public AvaliacaoResponse criar(@RequestBody @Valid AvaliacaoRequest request) {
        return service.criar(request);
    }

    @GetMapping                                                // GET /avaliacoes?autor=gab
    public List<AvaliacaoResponse> listar(@RequestParam(required = false) String autor) { ... }

    @GetMapping("/{id}")                                       // GET /avaliacoes/3
    public AvaliacaoResponse buscar(@PathVariable Long id) { ... }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)                     // 204
    public void excluir(@PathVariable Long id) { ... }
}
```

| Anotação | De onde vem o dado | Exemplo |
|---|---|---|
| `@PathVariable` | pedaço da URL | `/avaliacoes/{id}` |
| `@RequestParam` | query string | `/avaliacoes?autor=ana` |
| `@RequestBody` | corpo JSON | POST/PUT |
| `@Valid` | dispara as validações do DTO | 400 se falhar |

Status HTTP que você deve saber de cabeça:

| Código | Quando |
|---|---|
| 200 OK | GET, PUT que deu certo |
| 201 Created | POST criou algo |
| 202 Accepted | pedido aceito, processado depois (fila, Aula 09) |
| 204 No Content | DELETE deu certo, sem corpo |
| 400 Bad Request | entrada inválida |
| 401 Unauthorized | sem token / token inválido (Aula 08) |
| 403 Forbidden | tem token, mas não tem permissão (Aula 08) |
| 404 Not Found | recurso não existe |
| 500 | erro não tratado (bug) |
| 503 | uma dependência externa caiu |

Se você quiser controlar status e headers manualmente, devolva `ResponseEntity` (exemplo em `relatorio/RelatorioController`: `ResponseEntity.accepted().location(...).body(...)`).

### 6. Exceções: erro vira status HTTP

`exception/ErroHandler.java`

```java
@RestControllerAdvice      // "intercepta" exceções de TODOS os controllers
public class ErroHandler {
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(RecursoNaoEncontradoException e) {
        return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class)   → 400
    @ExceptionHandler(MethodArgumentNotValidException.class) → 400 (falha do @Valid)
}
```

O fluxo: o service lança `RecursoNaoEncontradoException`, ninguém faz try/catch, o `ErroHandler` pega e transforma em 404 com JSON. Assim o controller continua limpo.

Jeito mais curto (usado em `endereco/ServicoExternoIndisponivelException`): anotar a própria exceção com `@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)`.

---

## Receita: criar um CRUD do zero (ordem que eu seguiria na prova)

Exemplo: "API de cursos com GET (filtro por nome startsWith), POST e DELETE lógico".

1. **Entity** `Curso` com `id`, campos pedidos e `deletado`.
2. **Repository** `CursoRepository extends JpaRepository<Curso, Long>` com `findByDeletadoFalse`, `findByDeletadoFalseAndNomeStartingWith`, `findByIdAndDeletadoFalse`.
3. **DTOs** `CursoRequest` (com validações) e `CursoResponse` (com `from`).
4. **Exceção** `RecursoNaoEncontradoException` + `ErroHandler` (copie deste projeto).
5. **Service** com um método por rota. Lance exceção quando não achar.
6. **Controller** com as rotas e os status certos.
7. **Rodar e testar no Postman** (`docker compose up -d postgres` e rodar a aplicação).
8. **Padrão de projeto pedido** (Observer, Strategy, Factory...) num pacote com o nome do padrão.
9. **Testes de unidade** do service até 100% (ver Guia 2).
10. **Teste de integração** de pelo menos uma rota (ver Guia 2).
11. **Workflows** e deploy.

Faça **todas as rotas funcionando antes** de enfeitar. Na intermediária, a perda de 0,5 foi por ter só criar e listar.

## Erros comuns

- Regra de negócio no controller → difícil de testar, duplicada.
- Controller chamando repository direto → pula as regras do service.
- Devolver `null` quando não acha → vira 200 com corpo vazio. Lance exceção e devolva 404.
- Esquecer `@Valid` → as anotações do DTO são ignoradas.
- Esquecer `@Service`/`@Component`/`@RestController` → "No qualifying bean of type...".
- Entity sem construtor vazio → erro do Hibernate.
- `localhost` no `application.properties` rodando dentro do Docker → use o nome do container (Aula 05).
