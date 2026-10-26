# estudoProjSoft

Projeto de estudo de **Projeto de Software e Gestão Ágil** (Insper, 2026.2, Prof. Eduardo Santana).
Uma API Spring Boot que junta, num lugar só, tudo o que a disciplina pede, com cada parte comentada e ligada à aula correspondente.

## Por onde começar

1. **`guias/01-arquitetura-em-camadas.md`**: como organizar controller, service, DTO, repository e entity, e a receita para montar um CRUD do zero.
2. **`guias/02-testes.md`**: teste de unidade com Mockito, teste de integração com MockMvc e Testcontainers, e como chegar em 100% de cobertura.
3. **`aulas/`**: um arquivo por aula, explicando o conteúdo dos slides e apontando para o código.

| Aula | Arquivo | Onde está no código |
|---|---|---|
| 01 Introdução | `aulas/01-introducao.md` | (teoria) |
| 02 Manutenibilidade | `aulas/02-manutenibilidade.md` | `observer/`, `pagamento/` |
| 03 Docker + AWS | `aulas/03-docker-e-aws.md` | `Dockerfile`, `docker-compose.yml` |
| 04 Confiabilidade + GitHub Actions | `aulas/04-confiabilidade-e-github-actions.md` | `exception/`, `.github/workflows/` |
| 05 Redes Docker | `aulas/05-redes-docker.md` | `application.properties`, `docker-compose.yml` |
| 06 Testes | `aulas/06-testes-de-software.md` | `src/test/` |
| 07 Arquitetura distribuída | `aulas/07-arquitetura-distribuida.md` | `endereco/ViaCepClient`, `relatorio/` |
| 08 Segurança | `aulas/08-seguranca.md` | `config/SecurityConfig`, `SegurancaTests` |
| 09 Desempenho | `aulas/09-desempenho-e-escalabilidade.md` | `endereco/EnderecoCache`, `relatorio/` |

## Como é a prova (pela intermediária)

Prática, no Blackboard com inspeção. Implementar uma API Spring Boot e entregar **link do GitHub + prints**:

1. CRUD + padrão de projeto (Observer na intermediária): 4 pts
2. Testes de unidade com **100% de cobertura no service** + print do JaCoCo: 2 pts
3. **Teste de integração** de uma rota: 2 pts
4. **Deploy na AWS via GitHub Actions** + print do Postman com o IP: 2 pts

O domínio `Avaliacao` deste projeto é exatamente o enunciado da intermediária, resolvido.

## Estrutura

```
src/main/java/br/insper/estudo
├── controller/   AvaliacaoController                 (camada HTTP)
├── dto/          AvaliacaoRequest, AvaliacaoResponse (contrato da API)
├── service/      AvaliacaoService                    (regras de negócio)
├── repository/   AvaliacaoRepository, AuditoriaRepository
├── entity/       Avaliacao, Auditoria                (tabelas)
├── observer/     padrão Observer (auditoria + moderação)
├── exception/    exceções + ErroHandler (erro → status HTTP)
├── config/       SecurityConfig (OAuth2, profile "auth")
├── pagamento/    padrão Strategy + Factory           (Aula 02)
├── endereco/     REST síncrono + cache Redis         (Aulas 07 e 09)
└── relatorio/    fila assíncrona Redis + 202         (Aulas 07 e 09)
```

## Rodando

Precisa de **JDK 21+** e **Docker**. O Maven vem pelo wrapper (`./mvnw`, ou `mvnw.cmd` no Windows), não precisa instalar.

```bash
# opção 1: tudo no Docker
docker compose up -d --build

# opção 2: dependências no Docker, app pela IDE (IntelliJ: rodar EstudoApplication)
docker compose up -d postgres redis
./mvnw spring-boot:run

# testes + relatório de cobertura (abre tests/index.html no navegador)
./mvnw test
```

Os testes de integração sobem um Postgres sozinhos com Testcontainers: só precisa do Docker Desktop aberto.

## Rotas

| Método | Rota | O que faz | Status |
|---|---|---|---|
| POST | `/avaliacoes` | cria (`{"autor","conteudo","nota"}`) | 201 / 400 |
| GET | `/avaliacoes?autor=ga` | lista não deletadas, filtro "começa com" | 200 |
| GET | `/avaliacoes/{id}` | busca | 200 / 404 |
| DELETE | `/avaliacoes/{id}` | deleção lógica | 204 / 404 |
| POST | `/pagamentos` | `{"tipo":"PIX","valor":100}` (Strategy) | 200 |
| GET | `/enderecos/{cep}` | ViaCEP com cache Redis | 200 / 404 / 503 |
| POST | `/relatorios` | enfileira um relatório | 202 |
| GET | `/relatorios/{id}` | status do relatório | 200 / 404 |

## Testes

| Classe | Tipo | Ensina |
|---|---|---|
| `service/AvaliacaoServiceTests` | unidade | Mockito, 100% de cobertura, um teste por caminho |
| `observer/ObserversTests` | unidade | ArgumentCaptor, mock sem anotação |
| `endereco/EnderecoServiceTests` | unidade | `@InjectMocks` (estilo do professor), cache hit/miss |
| `pagamento/PagamentoFactoryTests` | unidade | testar uma factory |
| `controller/AvaliacaoControllerWebTests` | fatia web | `@WebMvcTest` + `@MockitoBean`, `@Valid`, erro → 404 |
| `controller/AvaliacaoControllerTests` | integração | `@SpringBootTest` + MockMvc + Testcontainers |
| `config/SegurancaTests` | integração | 401 x 403 com `jwt()` |

## CI/CD

- `ci.yml`: todo push compila e roda os testes. **Olhe a aba Actions depois do primeiro push.**
- `tests.yml`: em PR, comenta a cobertura e reprova abaixo de 80%.
- `deploy.yml`: deploy na EC2. Desligado até você criar a variável `DEPLOY_ENABLED=true` e os secrets (instruções no próprio arquivo).

## Versões

Spring Boot 4.1.0 (mesma dos repositórios do professor), Java 21 (o professor usa 25; o código roda nos dois), Testcontainers 2, JUnit 5, Mockito, JaCoCo 0.8.15.
