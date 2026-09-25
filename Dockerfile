# ==============================================================================
# Multi-stage Dockerfile para Constructora Backend (Java 21 Enterprise)
# ==============================================================================

# ─── Etapa 1: Construcción (Maven Build) ──────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Cachear dependencias de Maven
COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN ./mvnw dependency:go-offline -B || true

# Copiar código fuente y compilar artefacto productivo
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ─── Etapa 2: Imagen de Ejecución Ligera y Segura ─────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner

# Metadatos del contenedor
LABEL maintainer="Grupo Arquinova"
LABEL description="Backend REST API - Constructora Arquinova"

WORKDIR /app

# Crear usuario y grupo sin privilegios de root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Instalar utilidades básicas para healthcheck
RUN apk add --no-cache curl tzdata

# Configurar zona horaria de Colombia
ENV TZ=America/Bogota

# Copiar el binario empaquetado desde la etapa de compilación
COPY --from=builder /app/target/*.jar app.jar

# Asignar permisos al usuario no privilegiado
RUN chown -R appuser:appgroup /app

USER appuser

# Exponer el puerto del servicio
EXPOSE 8080

# Parámetros JVM optimizados para contenedores y Kubernetes
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Healthcheck interno del contenedor
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
