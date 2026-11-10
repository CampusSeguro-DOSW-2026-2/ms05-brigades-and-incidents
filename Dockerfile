
# Etapa 1: Construcción de la aplicación
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

# Descargar dependencias
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src ./src
COPY database ./database

# Construir el ejecutable
RUN ./mvnw clean package -DskipTests -B

# Etapa 2: Ejecución de la aplicación
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/brigades-and-incidents-0.0.1-SNAPSHOT.jar app.jar

# Directorio para evidencias
RUN mkdir -p /app/uploads/evidencias

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
