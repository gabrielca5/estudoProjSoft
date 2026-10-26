# Aula 09: Desempenho e Escalabilidade

> Código desta aula: `endereco/` (cache com Redis) e `relatorio/` (fila assíncrona + 202 Accepted).
> Repositório do professor: `partida` (`TimeCacheService` com HashMap e `RedisTimeCacheService` com TTL).

## A pergunta central

**O sistema mantém o nível de serviço quando o volume aumenta?**

Uma API só pode ser avaliada **sob uma carga definida**. "Minha API é rápida" sem dizer com quantos usuários não é uma medida comparável.

## Desempenho x escalabilidade

| | Pergunta | Exemplo |
|---|---|---|
| **Desempenho** | como o sistema responde para uma carga **conhecida**? | p95 de `GET /produtos` com 30 usuários |
| **Escalabilidade** | como a capacidade **muda** quando a carga ou os recursos aumentam? | adicionar instâncias sem saturar o banco |

Formas de escalar:
- **Vertical**: máquina maior (mais CPU/RAM). Simples, mas tem limite e custo alto.
- **Horizontal**: mais instâncias do serviço atrás de um load balancer. Escala mais, mas exige que o serviço não guarde estado em memória (ex.: o cache em HashMap do professor fica diferente em cada instância; o Redis resolve).

## Três medidas que se leem juntas

| Medida | Pergunta | Exemplo |
|---|---|---|
| **Latência** | quanto tempo **uma** chamada leva? | 240 ms |
| **Throughput** | quantas chamadas termina **por segundo**? | 80 req/s |
| **Concorrência** | quantas operações estão em andamento **ao mesmo tempo**? | 40 ativas |

Elas se relacionam (Lei de Little):

> **operações em andamento ≈ taxa de chegada × tempo médio no sistema**

Com 80 req/s chegando e 0,5 s de latência, há ~40 requisições em andamento. Se a latência dobra, a concorrência dobra (mais threads, mais conexões de banco) e o sistema pode entrar em colapso.

## Percentis: a média mente

Ordene todas as respostas da mais rápida para a mais lenta. **p90** é o tempo até o qual terminaram 90% das chamadas.

| p50 | p75 | p90 | p95 | p99 |
|---|---|---|---|---|
| 120 ms | 180 ms | 300 ms | 450 ms | 900 ms |

A maioria é rápida (p50 = 120 ms), mas 1 em cada 100 usuários espera quase 1 s. A média esconderia isso. Por isso requisitos sérios são escritos como "p95 < 300 ms", não "média < 300 ms".

## Três perfis de teste

| Perfil | Pergunta | Como |
|---|---|---|
| **Smoke** | o plano funciona? | poucas chamadas para validar a configuração |
| **Carga** | como responde no uso **esperado**? | sustentar a carga normal por um tempo |
| **Stress** | onde está o **limite**? | aumentar até degradar e observar como falha |

## Protocolo para comparar execuções

1. **Fixe o cenário**: endpoints, dados, proporção das operações.
2. **Fixe a carga**: threads, ramp-up, duração, pausas.
3. **Prepare o ambiente**: versão da API, banco, configuração do gerador.
4. **Execute e monitore**: p50/p95/p99, req/s, erros, CPU, conexões.
5. **Repita e compare**: mesmas condições, critérios definidos **antes**.

Se mudar duas coisas entre execuções, você não sabe qual delas causou a diferença.

## JMeter

Ferramenta open source de teste de carga. Um plano para API tem:

1. **Thread Group**: quantos usuários (threads), ramp-up (em quanto tempo sobem), duração
2. **HTTP Request**: método, host, caminho
3. **Assertion**: status e conteúdo esperados (senão você mede a velocidade de devolver erro)
4. **Resultados**: latência, throughput, erros

Rodar sem interface e gerar relatório HTML:

```bash
jmeter -n -t api-test.jmx -l resultados.jtl -e -o relatorio
```

Cuidado: o próprio JMeter pode virar o gargalo (CPU, memória, rede da máquina que gera a carga).

## Meça antes de otimizar

Comece pelo **sintoma** e procure evidência no componente que pode explicá-lo:

| Sintoma | Onde olhar |
|---|---|
| p95 sobe | latência da aplicação e das dependências |
| CPU alta | profiling, serialização, código caro |
| banco saturado | consultas, índices, pool de conexões |
| fila cresce | produtores mais rápidos que consumidores |
| JMeter no limite | CPU, memória e rede do gerador |

---

## Técnica 1: cache

Use quando **a mesma informação é lida com frequência** e pode ser reutilizada por um período.

```
Cliente → API → consulta o cache
                  ├─ HIT  → devolve na hora
                  └─ MISS → busca na origem (banco/serviço) → guarda no cache → devolve
```

No projeto (`EnderecoService`, padrão **cache-aside**):

```java
return cache.obter(cep).orElseGet(() -> {   // HIT? devolve
    Endereco e = client.buscar(cep);          // MISS: vai ao ViaCEP
    cache.guardar(cep, e);                    // guarda para a próxima
    return e;
});
```

E no `EnderecoCache`, a chave com prefixo e **TTL** (tempo de vida):

```java
redis.opsForValue().set("cep:" + cep, json, Duration.ofHours(1));
```

- **Benefício**: menos leituras repetidas, menos carga na origem, menor latência.
- **Riscos**: **dado desatualizado** (por isso TTL e invalidação) e **cache stampede** (o cache de um item popular expira e mil requisições vão à origem ao mesmo tempo).
- **Acompanhe**: taxa de acerto (hit rate), latência, carga no banco.

Decisão de confiabilidade no código: se o Redis cair, o `try/catch` faz o cache "sumir" e a API segue funcionando (só fica mais lenta). Cache é otimização, não pode virar ponto único de falha.

O professor mostra as duas versões: `TimeCacheService` com `HashMap` (simples, mas some ao reiniciar e não é compartilhado entre instâncias) e `RedisTimeCacheService` (compartilhado, com TTL de 24h).

## Técnica 2: comunicação assíncrona (fila)

Para tarefas que **podem terminar depois**: a API aceita o pedido, publica numa fila e responde na hora. Um **worker** processa.

```
POST /relatorios ─→ API valida e publica ─→ [fila] ─→ worker gera o relatório
      ←── 202 Accepted (pedido aceito para processamento)
GET /relatorios/{id} ─→ cliente consulta o estado (PENDENTE → PRONTO)
```

No projeto: `RelatorioController` devolve **202** com o `id` e o header `Location`; `RelatorioWorker` consome.

Cuidados:
- **worker fora do ar?** a fila cresce; monitore e recupere
- **retries e consistência eventual**: por um tempo o dado ainda não está pronto
- **mensagens podem ser processadas mais de uma vez**: o processamento precisa ser **idempotente** (o worker deste projeto confere se já está PRONTO antes de refazer)

## Cache x fila

| Técnica | Reduz | Principal cuidado |
|---|---|---|
| **Cache** | leituras repetidas e recomputação | validade e invalidação |
| **Fila** | espera da chamada por trabalho demorado | retries, idempotência, fila acumulando |

**Escolha a partir do gargalo medido** e do comportamento que o produto precisa garantir.

## Outras melhorias para investigar

| Área | O que olhar |
|---|---|
| Banco de dados | índices, consultas, volume de dados |
| Pool de conexões | espera por conexão, limite de concorrência |
| Payload e compressão | respostas grandes, custo de rede |
| CDN | conteúdo estático longe da origem |
| Escala horizontal | instâncias saturadas (com as dependências aguentando) |
| Profiling | trecho de código que consome CPU |

## Pratique

1. Com o compose rodando, chame `GET /enderecos/01310100` duas vezes e compare o tempo (a 2ª vem do Redis). Veja a chave: `docker compose exec redis redis-cli TTL cep:01310100`.
2. Monte no JMeter um plano com 30 threads para `GET /avaliacoes` e anote p50, p95 e p99.
3. Um sistema tem média de 150 ms e p99 de 4 s. Qual problema isso indica?
4. Um endpoint de relatório leva 20 s. Cache ou fila? Por quê?
