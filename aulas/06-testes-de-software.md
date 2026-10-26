# Aula 06: Testes de Software

> O passo a passo prático (Mockito, MockMvc, Testcontainers, como chegar em 100%) está em **`guias/02-testes.md`**. Aqui fica a teoria da aula.
> Código: `src/test/java/...`

## O que é testar

Verificar os requisitos, **funcionais e não funcionais**. Pode ser manual (alguém clicando) ou automatizado (código que testa código). A disciplina foca nos automatizados.

**O mais importante, segundo o professor: testes protegem contra quebras futuras.** O teste que você escreve hoje é o que avisa, daqui a 3 meses, que a mudança de outra pessoa quebrou sua regra.

## Por que fazer

- aumenta a confiança no código
- facilita a manutenção (Aula 02)
- evita bugs em produção (Aula 04, confiabilidade)
- permite **refatorar com segurança**: você muda a estrutura e os testes garantem que o comportamento continuou igual

## Tipos de teste

**Principais**

| Tipo | Testa | Neste projeto |
|---|---|---|
| **Unidade** | uma classe isolada; dependências viram mocks | `AvaliacaoServiceTests`, `ObserversTests`, `EnderecoServiceTests`, `PagamentoFactoryTests` |
| **Integração** | várias partes juntas (controller + service + banco real) | `AvaliacaoControllerTests`, `SegurancaTests` |
| **End-to-end (E2E)** | o sistema inteiro como o usuário usa (ex.: Selenium clicando no front) | não tem |

**Outros**

| Tipo | Pergunta |
|---|---|
| Carga | aguenta X usuários? (JMeter, Aula 09) |
| Contrato | a API continua respondendo no formato que os consumidores esperam? |
| Regressão | o que funcionava continua funcionando depois da mudança? |
| Segurança (pentest) | dá para invadir? |

### A pirâmide

Muitos testes de unidade (baratos e rápidos), alguns de integração, poucos E2E (lentos e frágeis). Se você só tem E2E, a suíte demora uma hora e quebra por qualquer mudança de layout.

## Testar cegamente não resolve

Ter 200 testes não significa nada se eles não passam pelas partes importantes. Por isso:

- **medir a cobertura** (JaCoCo): que porcentagem das linhas e dos ramos os testes executaram
- **medir a qualidade** dos testes (um teste sem `assert` executa a linha mas não verifica nada)
- usar a cobertura como **critério de aprovação de código**: o `tests.yml` deste repo reprova o PR se a cobertura ficar abaixo de 80%

Cuidado com o inverso: 100% de cobertura não prova que o código está certo, prova só que cada linha rodou pelo menos uma vez. Mas é o que a prova pede para o service.

## JaCoCo no projeto

Configurado no `pom.xml`:
- `prepare-agent`: instrumenta o código durante os testes
- `report` na fase `test`: gera o HTML em `tests/index.html` e o XML em `tests/jacoco.xml` (que o `tests.yml` lê)

```bash
./mvnw test        # ou mvn test
# abra tests/index.html no navegador
```

## Pratique

Faça os exercícios do final de `guias/02-testes.md`.
