# Guia 2: testes, do zero até 100% de cobertura

Na intermediária, testes valeram 4 dos 10 pontos (2 de cobertura no service e 2 de teste de integração). Este guia explica **o que** testar, **como** escrever e **como** chegar nos 100%.

## Os três tipos de teste deste projeto

```
                     ▲ mais lento, mais realista
   ┌───────────────────────────────┐
   │  Integração  (@SpringBootTest  │  app inteira + Postgres de verdade (Docker)
   │  + Testcontainers + MockMvc)   │  AvaliacaoControllerTests
   ├───────────────────────────────┤
   │  Fatia web  (@WebMvcTest)      │  só controller + validação + erros, service mockado
   │                                │  AvaliacaoControllerWebTests
   ├───────────────────────────────┤
   │  Unidade  (Mockito puro)       │  uma classe, dependências mockadas, milissegundos
   │                                │  AvaliacaoServiceTests, ObserversTests, EnderecoServiceTests
   └───────────────────────────────┘
                     ▼ mais rápido, mais isolado
```

| | Unidade | Fatia web | Integração |
|---|---|---|---|
| Sobe o Spring? | Não | Só a parte web | Inteiro |
| Banco | Mock | Não usa | Postgres real (Testcontainers) |
| Precisa de Docker? | Não | Não | **Sim** |
| Pergunta que responde | "a regra está certa?" | "a rota responde o status certo?" | "as camadas funcionam juntas?" |
| O que a prova pede | 100% no service | (opcional) | 1 teste da rota pedida |

---

## Parte 1: teste de unidade com Mockito

Abra `src/test/java/br/insper/estudo/service/AvaliacaoServiceTests.java` junto.

### O problema que o mock resolve

O `AvaliacaoService` depende do `AvaliacaoRepository` (que precisa de banco). Para testar **só** a lógica do service, trocamos o repository por um **mock**: um objeto falso em que você programa as respostas.

```java
@ExtendWith(MockitoExtension.class)   // liga o Mockito no JUnit 5
class AvaliacaoServiceTests {

    @Mock                               // cria um AvaliacaoRepository falso
    private AvaliacaoRepository repository;

    @Mock
    private AvaliacaoObserver observer;

    private AvaliacaoService service;

    @BeforeEach                         // roda antes de CADA @Test
    void setUp() {
        service = new AvaliacaoService(repository, List.of(observer));
    }
}
```

Por padrão um mock devolve "vazio": `null` para objetos, `0`, `false`, lista vazia, `Optional.empty()`... até você programar outra coisa.

### Estrutura de todo teste: Arrange, Act, Assert

```java
@Test
void buscarPorId_quandoExiste_deveRetornarDto() {
    // Arrange: prepara dados e programa o mock
    Mockito.when(repository.findByIdAndDeletadoFalse(1L))
           .thenReturn(Optional.of(entidade(1L, "gabriel", 5)));

    // Act: chama o método testado
    AvaliacaoResponse r = service.buscarPorId(1L);

    // Assert: confere o resultado
    Assertions.assertEquals(1L, r.id());
}
```

Nome do teste: `metodo_situacao_resultadoEsperado`. Quando falhar, o nome já diz o que quebrou.

### Vocabulário do Mockito (cola)

**Programar o mock (Arrange)**

```java
Mockito.when(repo.findById(1L)).thenReturn(Optional.of(a));       // devolve valor fixo
Mockito.when(repo.findById(99L)).thenReturn(Optional.empty());    // simula "não achou"
Mockito.when(repo.save(Mockito.any())).thenReturn(a);             // qualquer argumento
Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> {        // devolve algo baseado no argumento
    Avaliacao x = inv.getArgument(0); x.setId(1L); return x;
});
Mockito.when(client.buscar("123")).thenThrow(new RuntimeException("caiu"));  // simula erro

// métodos void usam a ordem invertida:
Mockito.doThrow(new RecursoNaoEncontradoException("x")).when(service).excluir(5L);
```

**Conferir as chamadas (Assert)**

```java
Mockito.verify(repo).save(a);                          // foi chamado 1 vez com "a"
Mockito.verify(repo, Mockito.times(2)).save(Mockito.any());
Mockito.verify(repo, Mockito.never()).save(Mockito.any());   // NÃO foi chamado
Mockito.verifyNoInteractions(observer);                 // ninguém tocou nesse mock

// ver o que foi passado ao mock:
ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
Mockito.verify(repo).save(captor.capture());
Assertions.assertFalse(captor.getValue().isDeletado());
```

**Asserts do JUnit**

```java
Assertions.assertEquals(esperado, atual);
Assertions.assertTrue(cond);  Assertions.assertFalse(cond);
Assertions.assertNull(x);     Assertions.assertNotNull(x);
Assertions.assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId(99L));
Assertions.assertInstanceOf(ProcessadorPix.class, obj);
```

**Matchers**: `Mockito.any()`, `anyLong()`, `anyString()`, `eq(valor)`. Regra chata: se usar um matcher num argumento, **todos** os argumentos daquela chamada precisam ser matchers:

```java
Mockito.verify(observer).atualizar(Mockito.any(), Operacao.CREATE);             // ERRO
Mockito.verify(observer).atualizar(Mockito.any(), Mockito.eq(Operacao.CREATE)); // certo
```

### @Mock + @InjectMocks (estilo do professor)

Quando o service usa `@Autowired` em campo, você não consegue passar os mocks no construtor. Aí:

```java
@Mock private EnderecoCache cache;
@Mock private ViaCepClient client;
@InjectMocks private EnderecoService service;   // cria o service e injeta os @Mock nele
```

Exemplo completo em `endereco/EnderecoServiceTests.java`.

---

## Parte 2: chegando em 100% de cobertura

Cobertura = quais linhas e **ramos** (cada lado de um `if`) foram executados pelos testes. O JaCoCo mede.

### Passo a passo

1. Liste os métodos públicos do service.
2. Para cada método, liste os **caminhos**: cada `if` verdadeiro e falso, cada `||`/`&&`, cada exceção, cada `orElseThrow`.
3. Escreva **um teste por caminho**.
4. Rode `./mvnw test` (ou `mvn test`) e abra `tests/index.html` no navegador.
5. Entre na classe do service. Linhas **verdes** = cobertas. **Amarelas** = ramo parcialmente coberto (passe o mouse no losango para ver quantos faltam). **Vermelhas** = nunca executadas.
6. Escreva o teste que falta, repita.

### Exemplo: o mapa de caminhos do AvaliacaoService

| Método | Caminho | Teste |
|---|---|---|
| `criar` | nota null | `criar_comNotaNula_deveLancarExcecaoESemSalvar` |
| | nota < 1 | `criar_comNotaMenorQue1_deveLancarExcecao` |
| | nota > 5 | `criar_comNotaMaiorQue5_deveLancarExcecao` |
| | nota válida (salva + notifica) | `criar_deveSalvarPreencherDataENotificarObservers` |
| `listar` | autor null | `listar_semFiltro_deveBuscarTodasNaoDeletadas` |
| | autor em branco | `listar_comFiltroEmBranco_deveIgnorarFiltro` |
| | autor preenchido | `listar_comFiltro_deveUsarStartsWith` |
| `buscarPorId` | existe | `buscarPorId_quandoExiste_deveRetornarDto` |
| | não existe | `buscarPorId_quandoNaoExiste_deveLancarNaoEncontrado` |
| `excluir` | existe | `excluir_deveMarcarComoDeletadaSalvarENotificar` |
| | não existe | `excluir_quandoNaoExiste_naoDeveSalvarNemNotificar` |

Repare: a condição `request.nota() == null || request.nota() < 1 || request.nota() > 5` tem **4 caminhos** (null, <1, >5, válido). É isso que deixa linha amarela quando falta um.

### A pegadinha da intermediária

O JaCoCo mostrava 100% porque o teste cobria uma classe pequena, mas o service de verdade não estava coberto. O que a prova pede é **100% na classe de serviço**. Abra a classe certa no relatório e confira linha por linha.

---

## Parte 3: teste de integração

Abra `controller/AvaliacaoControllerTests.java` e `IntegracaoBase.java`.

### Montagem

```java
@SpringBootTest            // sobe a aplicação inteira
@AutoConfigureMockMvc      // cria o MockMvc (um "Postman" dentro do teste)
class AvaliacaoControllerTests extends IntegracaoBase {   // IntegracaoBase sobe o Postgres
    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;   // objeto <-> JSON
}
```

`IntegracaoBase`:

```java
static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");
static { POSTGRES.start(); }                       // sobe um container Docker

@DynamicPropertySource                             // troca a URL do banco pela do container
static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
}
```

Dependências no `pom.xml`: `testcontainers-postgresql` e `testcontainers-junit-jupiter` (escopo `test`). **O Docker precisa estar rodando** na sua máquina. No GitHub Actions já tem.

### Anatomia de uma chamada MockMvc

```java
MvcResult resultado = mockMvc.perform(
        post("/avaliacoes")                                   // método + rota
            .contentType("application/json")                  // header
            .content(json.writeValueAsString(request)))       // corpo
    .andExpect(status().isCreated())                          // confere status
    .andExpect(jsonPath("$.autor").value("maria"))            // confere campo do JSON
    .andReturn();

AvaliacaoResponse criada = json.readValue(
        resultado.getResponse().getContentAsString(), AvaliacaoResponse.class);
```

- `get(...)`, `post(...)`, `put(...)`, `delete(...)` vêm de `MockMvcRequestBuilders` (import estático).
- `.param("autor", "ze")` adiciona query string.
- `status().isOk() / isCreated() / isNoContent() / isBadRequest() / isNotFound() / isUnauthorized() / isForbidden()`.
- `jsonPath("$.campo")`, `jsonPath("$[0].campo")`, `jsonPath("$.length()").value(2)`, `.exists()`, `.doesNotExist()`.

### O que vale testar na integração

- POST cria e devolve 201 com id preenchido (e grava no banco: `repository.findById(...)`).
- Entrada inválida devolve 400.
- GET com filtro devolve só o esperado.
- DELETE devolve 204 e depois o GET dá 404.
- Efeitos colaterais: o observer gravou a auditoria (`auditoriaRepository.count()` aumentou).

---

## Parte 4: fatia web (@WebMvcTest), opcional mas útil

`controller/AvaliacaoControllerWebTests.java`. Sobe só o controller, com o service trocado por mock via `@MockitoBean`. Bom para testar `@Valid`, status e o `ErroHandler` sem Docker:

```java
@WebMvcTest(AvaliacaoController.class)
@Import(SecurityConfig.class)
class AvaliacaoControllerWebTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean AvaliacaoService service;   // mock DENTRO do contexto do Spring
}
```

Diferença importante: `@Mock` é Mockito puro (sem Spring). `@MockitoBean` coloca o mock **dentro do Spring**, substituindo o bean real.

---

## Erros comuns e como resolver

| Erro | Causa | Solução |
|---|---|---|
| `UnnecessaryStubbingException` | você fez `when(...)` de algo que o teste não usou | apague o `when` sobrando |
| `InvalidUseOfMatchersException` | misturou matcher com valor cru | use `eq(valor)` nos outros argumentos |
| `NullPointerException` dentro do service no teste | faltou programar um mock (ele devolveu null) | adicione o `when(...).thenReturn(...)` |
| `Could not find a valid Docker environment` | Docker parado | abra o Docker Desktop |
| Teste de integração passa sozinho e falha junto com outros | testes compartilham o banco | use dados únicos por teste ou não dependa de contagens absolutas |
| JaCoCo não gera `tests/` | rodou só `compile` | rode `mvn test` ou `mvn verify` |

## Exercícios (faça na ordem)

1. Apague `listar_comFiltroEmBranco_deveIgnorarFiltro`, rode `mvn test` e ache a linha amarela no JaCoCo. Reescreva o teste.
2. Adicione uma rota `PUT /avaliacoes/{id}` (service + controller) e escreva os testes de unidade de todos os caminhos.
3. Escreva um teste de integração para o PUT.
4. Crie um terceiro observer (ex.: conta avaliações 5 estrelas) e prove com teste que o service chama ele **sem mudar o service**.
5. Refaça a Prova Intermediária do zero num projeto novo, cronometrando.
