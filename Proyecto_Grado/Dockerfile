# ==============================================================================
# Dockerfile Multi-Etapa para Proyecto_Grado (Spring Boot 4 / Java 21)
# ==============================================================================

# ------------------------------------------------------------------------------
# Etapa 1: Compilación y empaquetado (Build)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Instalar dependencias básicas si fueran necesarias
RUN apk add --no-cache dos2unix

# Copiar el wrapper de Maven y el descriptor del proyecto
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Convertir saltos de línea CRLF a LF por si se construyó en Windows y dar permisos
RUN dos2unix mvnw && chmod +x mvnw

# Copiar el código fuente
COPY src/ ./src/

# Empaquetar el proyecto en un archivo JAR (omitiendo tests para agilizar el build del contenedor)
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------------------
# Etapa 2: Imagen final de ejecución liviana (Runtime)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Crear un usuario no privilegiado por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar el artefacto generado en la etapa de compilación
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto por defecto de Spring Boot
EXPOSE 8080

# Parámetros JVM optimizados para contenedores (ajusta memoria según límites del host)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Comando de ejecución
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
