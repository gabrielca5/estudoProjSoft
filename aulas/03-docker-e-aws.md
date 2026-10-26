# Aula 03: Docker (e tutorial AWS)

> Arquivos desta aula: `Dockerfile`, `docker-compose.yml`, `.dockerignore`.

## Por que Docker

"Na minha máquina funciona." O app depende de versão do Java, do Postgres, de variáveis de ambiente... Docker empacota a aplicação **com tudo que ela precisa** numa **imagem**, que roda igual em qualquer lugar (seu PC, o GitHub Actions, a EC2 da AWS).

| Conceito | Analogia | Exemplo |
|---|---|---|
| **Imagem** | a "receita" congelada, só leitura | `postgres:15-alpine`, `seuuser/estudo-projsoft:abc123` |
| **Container** | a imagem rodando (um processo isolado) | `docker run ... postgres` |
| **Dockerfile** | o script que monta a imagem | o arquivo `Dockerfile` |
| **Registry** | "GitHub de imagens" | Docker Hub |

Uma imagem pode virar vários containers.

## Comandos principais (cola do professor, explicada)

```bash
# criar e iniciar um container
docker run -d --name postgres-aula \
  -e POSTGRES_DB=auladb -e POSTGRES_USER=usuario -e POSTGRES_PASSWORD=senha \
  -p 5432:5432 \
  postgres
```

| Flag | O que faz |
|---|---|
| `-d` | roda em segundo plano (detached) |
| `--name` | dá um nome (opcional, mas sempre use; é como você vai se referir a ele) |
| `-p PORTA_MAQUINA:PORTA_APP` | expõe a porta. Sem isso você não acessa a aplicação de fora |
| `-e CHAVE=valor` | variável de ambiente |
| `--network rede` | coloca o container numa rede Docker (Aula 05) |

```bash
docker ps              # containers rodando
docker ps --all        # inclusive os parados
docker logs NOME       # ver o log (primeira coisa a olhar quando algo não sobe)
docker stop NOME       # para (não apaga)
docker rm NOME         # apaga o container (ocupa espaço, apague os que não usa)
docker rm --force NOME # para e apaga
docker images          # imagens baixadas
docker rmi IMAGEM      # apaga imagem

docker build -t usuario/nome-imagem .   # monta a imagem a partir do Dockerfile da pasta atual
docker push usuario/nome-imagem         # envia para o Docker Hub (precisa do usuário no nome)

docker network create -d bridge rede    # cria uma rede (Aula 05)
```

## O Dockerfile deste projeto (multi-stage)

```dockerfile
# Estágio 1: imagem GRANDE com Maven + JDK, só para compilar
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline     # baixa as dependências
COPY src src
RUN mvn -B clean package -DskipTests # gera target/*.jar

# Estágio 2: imagem PEQUENA só com o JRE + o jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Por que dois estágios? O Maven e o JDK são pesados e só servem para compilar. A imagem final leva só o necessário para **rodar**: fica menor, sobe mais rápido e tem menos coisa que possa ter vulnerabilidade.

Por que copiar o `pom.xml` antes do `src`? Cache de camadas: se você só mudou código, o Docker reaproveita a camada das dependências e não baixa tudo de novo.

`-DskipTests`: os testes rodam no CI (Aula 04), não precisam rodar de novo dentro do build da imagem.

## docker compose

Em vez de 3 `docker run` compridos, o `docker-compose.yml` descreve tudo:

```bash
docker compose up -d --build   # sobe postgres + redis + app
docker compose logs -f app     # acompanha o log do app
docker compose down            # derruba tudo
docker compose up -d postgres redis   # só as dependências (para rodar o app pela IDE)
```

## Tutorial AWS (resumo)

A disciplina usa uma máquina EC2 (Ubuntu) para o deploy. O fluxo:

1. Criar a instância EC2 e baixar a chave `.pem`.
2. Liberar no **Security Group** as portas que você vai usar (22 para SSH, 8080 para a API).
3. Acessar: `ssh -i chave.pem ubuntu@IP_DA_MAQUINA`.
4. Instalar o Docker na máquina.
5. Subir o Postgres com `docker run` (como acima).
6. Rodar a aplicação: `docker run -d -p 8080:8080 --name app usuario/imagem`.
7. Testar no Postman: `http://IP_DA_MAQUINA:8080/avaliacoes`.

Na prova: print do Postman mostrando o **IP da AWS** na URL e a resposta funcionando.

## Pratique

1. `docker compose up -d --build` e faça um POST em `localhost:8080/avaliacoes`.
2. `docker logs` no container do app e ache a linha do `ModeracaoObserver` depois de criar uma avaliação com nota 1.
3. Explique por que a imagem final não tem Maven.
