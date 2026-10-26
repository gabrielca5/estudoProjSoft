# Aula 01: Introdução (requisitos de qualidade e arquitetura)

## A pergunta que abre a disciplina

"Professor, só fazemos CRUD!" Um e-commerce, um banco, uma rede social: no fundo são CRUDs (criar, listar, atualizar, excluir). Então o que torna Mercado Livre, Nubank ou Instagram difíceis de construir?

Não é **o que** eles fazem. É **o que eles aguentam**:

- muitos usuários e muitas requisições por segundo
- muito dado
- ficar no ar 24x7
- ser alvo de ataques
- falha = dinheiro perdido
- picos de uso (Black Friday, horário de almoço)
- depender de serviços de terceiros
- mudar o tempo todo, com várias equipes mexendo

## Requisitos funcionais x requisitos de qualidade

**Funcional**: o que o sistema faz. "Cadastrar usuário", "realizar compra". Costuma ser simples de implementar, ainda mais com IA.

**De qualidade** (também chamados de não funcionais, ou "-ilities"): **como** o sistema faz e **o que suporta**. Exemplos de frases de requisito de qualidade:

- "deve suportar 10 mil requisições por segundo" (escalabilidade)
- "o usuário precisa estar autenticado" (segurança)
- "se um token vazar, o invasor não pode assumir a conta" (segurança)
- "o saldo nunca pode ficar inconsistente" (confiabilidade)
- "deploy várias vezes por dia" (manutenibilidade)

A disciplina inteira é sobre esses requisitos. Os principais e as técnicas para atingir cada um:

| Requisito | O que garante | Requisitos derivados | Como atingir (aparece em qual aula) |
|---|---|---|---|
| **Confiabilidade** | executa e devolve o valor certo | resiliência, disponibilidade, tolerância a falhas, recuperabilidade, consistência | idempotência, retry e timeout, circuit breaker, replicação, backup, testes (04, 06, 09) |
| **Escalabilidade** | desempenho mesmo com a carga variando | desempenho, elasticidade, throughput, latência, capacidade | cache, filas, load balancer, sharding, protocolo REST x gRPC (07, 09) |
| **Segurança** | processo seguro do desenvolvimento à execução | autenticação, autorização, confidencialidade, integridade, auditabilidade | OAuth2, tokens seguros, rate limit, secrets, checar dependências (08) |
| **Manutenibilidade** | evoluir a aplicação é fácil | testes, padronização | padrões de projeto, boas práticas, testes, bom processo (02, 06) |

Repare que **observabilidade** (logs, métricas, monitoramento) aparece como derivado de quase todos.

## Trade-off: não existe solução perfeita

Toda decisão resolve um problema e cria outro. Exemplos do slide:

- uma ferramenta resolve 100% do problema mas custa uma fortuna; outra resolve 95% e custa pouco
- uma linguagem é tecnicamente melhor, mas ninguém do time conhece
- um cache deixa a leitura rápida, mas agora o dado pode estar desatualizado (Aula 09)
- uma fila tira a espera do usuário, mas o processamento deixa de ser em tempo real (Aula 07)

**O papel do arquiteto é avaliar trade-offs e decidir o caminho.** Em qualquer questão dissertativa, a resposta boa sempre cita o ganho **e** o custo.

## O que é arquitetura de software

Três definições citadas (Garlan e Shaw; Kruchten; *Fundamentals of Software Architecture*). Elas dizem a mesma coisa de jeitos diferentes: estrutura de alto nível, decomposição em componentes, comunicação entre eles, e as características de qualidade que o sistema precisa ter.

A síntese do professor, que vale decorar:

> **Arquitetura de software é tomar decisões estruturais para que um sistema atenda seus requisitos de qualidade, dentro das restrições existentes (custo, tempo de desenvolvimento, ...).**

## E a IA?

- Acelera a engenharia e normalmente implementa com menos falhas que humanos.
- Mas não é tão boa em avaliar trade-off, não entende todos os riscos e não prevê as consequências de cada decisão. A decisão final continua humana.

## Organização da disciplina

| Parte | Tema |
|---|---|
| 1 | modelos arquiteturais, papel do arquiteto, requisitos não funcionais |
| 2 | manutenibilidade (padrões, testes, ferramentas) |
| 3 | confiabilidade (CI/CD, testes, monitoramento) |
| 4 | escalabilidade (sem explodir custo de infra) |
| 5 | segurança (autenticação, autorização) |

**Avaliação**: 2 provas valendo 50% da nota final (a final é em 09/11 ou 11/11) + projeto (10% durante o semestre e 40% na apresentação final).

## Perguntas para se testar

1. Dê dois exemplos de requisito funcional e dois de qualidade para um app de delivery.
2. Por que "suportar 5 mil pedidos por minuto no almoço" não é requisito funcional?
3. Cite um trade-off de usar cache.
4. Explique a definição de arquitetura do professor com suas palavras.
