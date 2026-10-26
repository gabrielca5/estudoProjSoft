# Aula 07: Arquitetura Distribuída

> Código desta aula: `endereco/ViaCepClient.java` (comunicação síncrona) e `relatorio/` (comunicação assíncrona com fila).
> Repositórios do professor: `partida` (Java) chama `time` (Python) por REST e manda resultados por fila Redis para o `worker.py`.

## O que é

Uma aplicação com **dois ou mais componentes que se comunicam pela rede**. Um back-end separado do front-end já é o mínimo de arquitetura distribuída. Dá para ir muito além, especializando serviços por funcionalidade:

| Streaming | E-commerce | Banco |
|---|---|---|
| usuários, assinaturas, planos, catálogo, streaming, sugestões, eventos, analytics | usuários, catálogo, carrinho, vendas, pagamento, logística, sugestões | usuários, conta corrente, investimentos, seguro, cartões, crédito, risco |

## Por que dividir (e o preço)

**Vantagens**
- às vezes é **necessidade**: o serviço é de outra empresa (gateway de pagamento, ViaCEP)
- manutenção e desenvolvimento mais simples (cada time cuida do seu serviço)
- **escalabilidade localizada**: só o catálogo sofre na Black Friday? escala só ele
- melhor **tolerância a falhas**: se sugestões cai, o checkout continua

**Desvantagens**
- problemas de rede: custo, erros, instabilidade, latência
- gerenciar várias aplicações (deploy, versão, monitoramento)
- custo maior de execução

É trade-off (Aula 01) clássico.

## O ponto central: comunicação entre serviços

### Síncrona: pergunta e espera a resposta

```
Serviço A ──requisição──→ Serviço B
          ←──resposta────
(A fica parado esperando)
```

**Como**: Socket, SOAP, **REST**, **gRPC**.
**Problemas**: falha de conexão, indisponibilidade do outro lado, problemas de rede e **dependência direta** (se B cai, A cai junto).

No projeto: `ViaCepClient` chama a API pública do ViaCEP.

```java
this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();

Endereco e = restClient.get()
        .uri("/ws/{cep}/json/", cep)
        .retrieve()
        .body(Endereco.class);   // JSON → record
```

Dois cuidados de **confiabilidade** que o código mostra:
1. **Timeout** (`setConnectTimeout`, `setReadTimeout`). Sem timeout, se o ViaCEP travar, cada requisição da sua API fica presa esperando, as threads acabam e sua API cai junto.
2. **Tratar a falha**: `RestClientException` vira `ServicoExternoIndisponivelException` (503). O cliente da sua API entende que o problema é uma dependência, não a entrada dele.

Teste: `GET localhost:8080/enderecos/01310100`.

### Assíncrona: deixa um recado e segue a vida

```
Serviço A ──produz mensagem──→ [ intermediário (fila) ] ──lê mensagem──→ Serviço B
(A não espera)                                          (B processa quando puder)
```

**Como**: **filas** (Redis, RabbitMQ, Kafka, SQS), arquivos. Banco de dados como fila: **não façam!**
**Problemas**: o processamento não é em tempo real, mensagens podem **acumular** na fila, mais complexidade, mais uma ferramenta para gerenciar.

No projeto: pacote `relatorio/`, mesmo esquema do professor (`PartidaProducer` + `worker.py`).

```java
// produtor (RelatorioFila.publicar)
redis.opsForHash().putAll("relatorio:" + id, Map.of("status", "PENDENTE"));
redis.opsForList().rightPush("fila:relatorios", id);   // coloca no FIM da lista

// consumidor (RelatorioWorker.consumir, roda a cada 1s)
String id = redis.opsForList().leftPop("fila:relatorios");  // tira do COMEÇO → FIFO
```

No professor o consumidor é Python: `redis_client.blpop('partidas', timeout=0)`. O `b` de **b**lpop é "blocking": espera até chegar mensagem.

## Quando usar cada uma

| Use síncrona quando... | Use assíncrona quando... |
|---|---|
| precisa da resposta **agora** para continuar (buscar dados do time para montar a partida) | o trabalho pode terminar **depois** (gerar relatório, mandar e-mail, atualizar ranking) |
| a operação é rápida | a operação é demorada ou pode ter pico |
| | quer desacoplar: o consumidor pode estar fora do ar e as mensagens esperam |

No exemplo do professor: criar partida **precisa** do nome e estádio do time agora (REST síncrono). Atualizar os pontos do time **pode** ser depois (fila).

## Conclusão do slide

- Entender arquitetura distribuída é essencial: mesmo que seu app não seja distribuído, ele provavelmente vai falar com serviços externos.
- Saber a diferença entre síncrona e assíncrona e **quando usar cada uma**.

## Pratique

1. `docker compose up -d --build`, faça `POST /relatorios` e logo depois `GET /relatorios/{id}` (PENDENTE). Espere 3s e repita (PRONTO).
2. Pare o worker (`FILA_WORKER=false`), crie 3 relatórios e veja a fila acumular: `docker compose exec redis redis-cli LRANGE fila:relatorios 0 -1`.
3. Mude `VIACEP_URL` para um endereço que não existe e veja o 503.
