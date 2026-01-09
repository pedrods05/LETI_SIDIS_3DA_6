# --- Estágio 1: Build (Compilação) ---
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app

# Copiar apenas o pom.xml primeiro para aproveitar o cache de dependências
COPY pom.xml .
# Baixar dependências (se o pom não mudou, o Docker usa o cache aqui)
RUN mvn dependency:go-offline

# Copiar o código fonte e compilar
COPY src ./src
# Faz o package saltando os testes (os testes correm antes no CI/CD)
RUN mvn clean package -DskipTests

# --- Estágio 2: Runtime (Execução Leve) ---
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Criar um utilizador não-root para segurança (Requisito da Aula 11)
RUN addgroup -S hapgroup && adduser -S hapuser -G hapgroup
USER hapuser

# Copiar o JAR compilado do estágio anterior
COPY --from=build /app/target/*.jar app.jar

# Expor a porta (opcional, mas boa prática)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]