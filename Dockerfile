# Aula 03/05: imagem em DOIS ESTÁGIOS (multi-stage build)
# Estágio 1: imagem grande com Maven + JDK, só para compilar
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline        # baixa dependências (fica em cache se o pom não mudar)
COPY src src
RUN mvn -B clean package -DskipTests    # testes rodam no CI, não no build da imagem

# Estágio 2: imagem pequena só com o JRE + o .jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
