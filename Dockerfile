FROM openjdk:17-jre-slim

# Instalar utilidades
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    && rm -rf /var/lib/apt/lists/*

# Directorio de trabajo
WORKDIR /app

# Copiar proyecto
COPY . /app/

# Dar permisos al gradlew
RUN chmod +x gradlew

# Compilar el plugin
RUN ./gradlew clean build -x test

# Crear directorios necesarios
RUN mkdir -p /opt/besu/plugins \
    && mkdir -p /var/log/besu \
    && chmod 755 /var/log/besu

# Copiar JAR compilado a plugins
RUN cp build/libs/besu-permission-rpc-plugin-2.0.0-fat.jar /opt/besu/plugins/

# Exponer puertos
EXPOSE 8545 9090 9091 30303

# Variables de entorno
ENV BESU_PLUGIN_DIR=/opt/besu/plugins
ENV BESU_LOG_DIR=/var/log/besu

# Mensaje de inicio
RUN echo "✓ Besu Permission RPC Plugin Docker Image (v2.0.0)"
RUN echo "✓ Compatible con Besu 25.8.0+"
RUN echo "✓ Plugin compilado y listo en: /opt/besu/plugins/"

# Comando por defecto
CMD ["echo", "Plugin listo para usar con Besu 25.8.0+. Monta este volumen en tu contenedor Besu."]
