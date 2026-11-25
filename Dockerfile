# Etapa 1: Construcción y verificación de la aplicación
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src ./src
COPY database ./database

# Ejecuta las pruebas antes de generar el JAR
RUN ./mvnw clean verify -B

# Etapa 2: Imagen de ejecución
FROM eclipse-temurin:21-jre

WORKDIR /app

# Usuario sin privilegios para ejecutar la aplicación
RUN mkdir -p /app/uploads/evidencias \
    && chown -R 10001:10001 /app/uploads

COPY --from=build --chown=10001:10001 \
    /app/target/brigades-and-incidents-0.0.1-SNAPSHOT.jar \
    app.jar

# Directorio utilizado por el almacenamiento de evidencias
ENV EVIDENCIAS_DIRECTORIO=/app/uploads/evidencias

# Puerto interno de Spring Boot
EXPOSE 8085

# No ejecutar el proceso como root
USER 10001:10001

ENTRYPOINT ["java", "-jar", "app.jar"]
