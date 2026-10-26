# Aula 04: Confiabilidade e GitHub Actions (CI/CD)

> Arquivos desta aula: `exception/ErroHandler.java`, `.github/workflows/ci.yml`, `tests.yml`, `deploy.yml`.

## Confiabilidade

Capacidade do software de **funcionar corretamente e de forma consistente**. Afeta desenvolvimento, manutenção e operação.

Como medir:
- **Disponibilidade**: tempo no ar.
- **Taxa de respostas com sucesso**.

### A tabela dos "noves" (disponibilidade)

| SLA | Fora do ar por ano | Por mês |
|---|---|---|
| 99% (two nines) | ~3 dias 15h | ~7h 18m |
| 99,9% (three nines) | ~8h 45m | ~43m |
| 99,99% (four nines) | ~52m | ~4m 23s |
| 99,999% (five nines) | ~5m 15s | ~26s |
| 99,9999% (six nines) | ~31s | ~2,6s |

Cada "nove" a mais custa muito mais caro (redundância, monitoramento, plantão). É trade-off: um sistema acadêmico não precisa de five nines; um sistema de pagamentos talvez sim.

### O que aumenta a confiabilidade

1. **Testes** (Aula 06): verificar se faz o que foi especificado. Manuais x automatizados. Bibliotecas: JUnit, Mockito, pytest, Selenium.
2. **Tratamento de erros**
3. **Integração Contínua (CI)**
4. **Entrega Contínua (CD)**
5. **Automação de tarefas**
6. **Monitoramento**: aplicações, servidores, bancos. Ferramentas: Grafana, Logstash, DataDog.

## Tratamento de erros

Toda linguagem tem mecanismo para isso (`try/catch` no Java). Além disso, bibliotecas de validação como o `spring-boot-starter-validation` (`@NotBlank`, `@Min`...).

O ponto do slide: tratar erro **não é só para humanos**. Em comunicação máquina-máquina, quem chama sua API é outro programa que decide o que fazer **pelo status HTTP**. Devolver 500 quando era 404, ou 200 com `{"erro": ...}`, quebra quem consome.

No projeto, o `ErroHandler` (`@RestControllerAdvice`) transforma exceções em status certos:

| Exceção | Status |
|---|---|
| `RecursoNaoEncontradoException` | 404 |
| `IllegalArgumentException` | 400 |
| `MethodArgumentNotValidException` (falha do `@Valid`) | 400 |
| `ServicoExternoIndisponivelException` | 503 |

## CI: Integração Contínua

Usar um repositório central (Git) onde todo mundo integra código com frequência. **Só usar Git não basta**: precisa de boas práticas e automação. Toda vez que alguém sobe código, uma máquina compila e roda os testes. Se quebrou, todo mundo fica sabendo na hora.

## CD: Entrega Contínua

Entregar código o tempo todo, de forma simples e **automatizada**. Nada de deploy manual (que é lento e sujeito a erro humano). E **parar o processo se algum problema aparecer** (teste falhou = não faz deploy).

Ferramentas: **GitHub Actions** (a da disciplina), Jenkins, CircleCI.

Tarefas que a disciplina automatiza:
- rodar os testes quando um PR é aberto
- exigir cobertura mínima
- criar a imagem Docker e subir no Docker Hub
- fazer o deploy automático no servidor

---

## GitHub Actions: anatomia de um workflow

Arquivos YAML em `.github/workflows/`. Quatro partes (igual ao material da aula):

### 1. Trigger (`on`): quando roda

```yaml
on:
  push:
    branches: ["main"]       # a cada push na main
  pull_request:
    branches: ["main"]       # a cada PR para a main
  workflow_dispatch:         # botão "Run workflow" manual
```

### 2. Jobs: o que roda e onde

```yaml
jobs:
  build:
    runs-on: ubuntu-latest   # SO da máquina (Linux, Windows, macOS; preços diferentes)
    permissions:             # o que o workflow pode fazer no repo
      contents: read
      pull-requests: write   # necessário para comentar a cobertura no PR
```

### 3. Steps: os passos, em sequência

```yaml
    steps:
      - uses: actions/checkout@v4          # baixa o código do repo
      - uses: actions/setup-java@v4        # instala o Java
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - run: mvn -B verify                 # comando de terminal
```

`uses:` = plugin pronto (uma "action"). `with:` = parâmetros do plugin. `run:` = comando de shell.

### 4. Deploy com Docker + SSH

```yaml
      - uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      - uses: docker/setup-buildx-action@v3
      - uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: usuario/app:${{ github.sha }}   # tag = hash do commit (cada versão é única)
      - uses: appleboy/ssh-action@master        # entra na EC2 e roda comandos
        with:
          host: ${{ secrets.HOST_TEST }}
          username: ubuntu
          key: ${{ secrets.KEY_TEST }}
          script: |
            docker rm --force app
            docker run -d -p 8080:8080 --network rede --name app usuario/app:${{ github.sha }}
```

`${{ secrets.X }}`: valor secreto configurado em **Settings > Secrets and variables > Actions**. Senha, IP e chave **nunca** vão no código (Aula 08).

Por que tag com `github.sha`? Cada commit gera uma imagem diferente; dá para saber exatamente qual versão está no ar e voltar para a anterior.

## Os 3 workflows deste repo

| Arquivo | Quando | Faz |
|---|---|---|
| `ci.yml` | todo push | compila e roda todos os testes, guarda o relatório JaCoCo como artefato |
| `tests.yml` | todo PR para main | roda testes e **comenta a cobertura no PR**; falha se < 80% (quality gate) |
| `deploy.yml` | push na main (se `DEPLOY_ENABLED=true`) | build → imagem → Docker Hub → SSH na EC2 → troca o container |

Para ativar o deploy: veja o comentário no topo do `deploy.yml` (secrets necessários e o que rodar uma vez na EC2).

## Pratique

1. Crie uma branch, mude algo, abra um PR e veja o `tests.yml` comentar a cobertura.
2. Quebre um teste de propósito e veja o PR ficar vermelho.
3. Por que o deploy usa `mvn package` (que roda testes) antes de gerar a imagem?
