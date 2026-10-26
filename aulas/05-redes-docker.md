# Aula 05: Redes Docker e variáveis de ambiente

> Arquivos desta aula: `src/main/resources/application.properties`, `docker-compose.yml`.

## O problema

Você roda o Postgres num container e a aplicação Java em outro. Na sua máquina (sem Docker) a URL `jdbc:postgresql://localhost:5432/auladb` funciona. Dentro do container, **não funciona**. Por quê?

Cada container tem o **próprio ambiente de rede**. Dentro do container da aplicação, `localhost` é **o próprio container da aplicação**. Não é o seu computador, nem o container do Postgres, nem nenhum outro.

```
 ┌──────── seu PC ───────────────────────────────┐
 │                                               │
 │  ┌─ container app ─┐    ┌─ container postgres ┐│
 │  │ localhost = EU  │ ✗→ │  porta 5432         ││
 │  └─────────────────┘    └─────────────────────┘│
 └───────────────────────────────────────────────┘
```

## A solução: uma rede bridge

Uma rede Docker do tipo **bridge** funciona como uma rede privada virtual. Containers na mesma rede se enxergam **pelo nome**, porque o Docker mantém um **DNS interno**.

```bash
docker network create -d bridge rede

docker run -d --name postgres-aula --network rede \
  -e POSTGRES_DB=auladb -e POSTGRES_USER=usuario -e POSTGRES_PASSWORD=senha \
  -p 5432:5432 postgres

docker run -d --name app --network rede -p 8080:8080 \
  -e DB_HOST=postgres-aula usuario/app
```

Agora a aplicação acessa `postgres-aula:5432`, e o DNS do Docker resolve o nome para o IP do container.

```
 ┌──────────── rede "rede" (bridge) ────────────┐
 │  app  ──── jdbc:postgresql://postgres-aula ──→ postgres-aula │
 └──────────────────────────────────────────────┘
```

O `-p 5432:5432` no Postgres só é necessário se **você** (fora do Docker) quiser acessar o banco. Entre containers da mesma rede, não precisa.

## O roteiro da aula, passo a passo

1. Subir o Postgres e rodar a aplicação **fora** do Docker com `localhost` → funciona.
2. Rodar a aplicação **dentro** do Docker com `localhost` → não funciona (motivo acima).
3. Criar a rede, colocar os dois nela e trocar a URL para `postgres-aula` → funciona.
4. Problema: agora para rodar fora do Docker você precisa trocar a URL de volta. Solução: **variáveis de ambiente**.

## Variáveis de ambiente com valor padrão

`application.properties` deste projeto:

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:auladb}
spring.datasource.username=${DB_USER:usuario}
spring.datasource.password=${DB_PASSWORD:senha}
```

`${DB_HOST:localhost}` = "use a variável de ambiente `DB_HOST`; se não existir, use `localhost`".

- Rodando pela IDE: nenhuma variável definida, usa `localhost`.
- Rodando no Docker: `-e DB_HOST=postgres-aula`.
- Rodando na AWS pelo GitHub Actions: `-e DB_PASSWORD=${{ secrets.DB_PASSWORD }}`.

Mesmo `.jar`, configurações diferentes. E a senha de produção nunca aparece no código (Aula 08: gerenciamento de segredos).

## No docker compose isso é automático

O compose cria uma rede para os serviços do arquivo, e cada serviço é acessível **pelo nome do serviço**:

```yaml
services:
  postgres: ...
  redis: ...
  app:
    environment:
      DB_HOST: postgres   # nome do serviço acima
      REDIS_HOST: redis
```

## Outras configs do projeto que usam a mesma ideia

```properties
spring.data.redis.host=${REDIS_HOST:localhost}
fila.worker.enabled=${FILA_WORKER:true}
viacep.url=${VIACEP_URL:https://viacep.com.br}
```

## Pratique

1. Rode o app pela IDE com `docker compose up -d postgres redis`. Qual host ele usa para o banco?
2. Rode tudo com `docker compose up -d --build`. Qual host o app usa agora? Onde isso foi definido?
3. Explique para alguém por que `localhost` não funciona dentro do container.
