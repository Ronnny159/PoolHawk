# ---------- Etapa 1: compilar con Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -B -q clean package -DskipTests -pl billiard-api -am

# ---------- Etapa 2: ejecutar con JRE ligero ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/billiard-api/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]