# 🐳 Docker - Besu Permission RPC Plugin

## Opción 1: Docker Compose (Recomendado)

Todo en un comando: Besu + Plugin + Prometheus + Grafana + Webhook

### Requisitos

- Docker 20.10+
- Docker Compose 2.0+

### Inicio Rápido

```bash
# 1. Compilar el proyecto (si no lo has hecho)
./gradlew build

# 2. Construir la imagen Docker
docker-compose build

# 3. Iniciar todos los servicios
docker-compose up -d

# 4. Verificar estado
docker-compose ps
```

### Acceder a los Servicios

- **Besu RPC**: http://localhost:8545
- **Prometheus**: http://localhost:9000
- **Grafana**: http://localhost:3000 (user: admin, password: admin)
- **Webhook**: http://localhost:8000

### Probar el Plugin

```bash
# 1. Desde otra terminal, ejecutar un evento de permisos
curl -X POST \
  --data '{"jsonrpc":"2.0","method":"perm_addAccountsToAllowlist","params":[["0xb9b81ee349c3807e46bc71aa2632203c5b462032"]],"id":1}' \
  http://127.0.0.1:8545/ \
  -H "Content-Type: application/json"

# 2. Ver los eventos capturados
curl http://localhost:8000/events | jq

# 3. Ver estadísticas
curl http://localhost:8000/stats | jq

# 4. Ver logs del plugin
docker-compose logs besu | grep "PERMISSION EVENT"
```

### Ver Logs

```bash
# Logs de Besu + Plugin
docker-compose logs -f besu

# Logs del webhook
docker-compose logs -f webhook-receiver

# Logs de Prometheus
docker-compose logs -f prometheus
```

### Detener Servicios

```bash
# Parar todos los servicios
docker-compose down

# Parar y eliminar volúmenes (cuidado: pierde datos)
docker-compose down -v
```

### Configuración Avanzada

Edita `docker-compose.yml` para:

1. **Cambiar puertos:**
```yaml
ports:
  - "8545:8545"    # RPC
  - "9000:9090"    # Prometheus
  - "3001:3000"    # Grafana
```

2. **Agregar variables de entorno:**
```yaml
environment:
  - BESU_PERMISSION_METRICS_PORT=9090
  - BESU_PERMISSION_LOG_FILE=/var/log/besu/events.log
```

3. **Montar archivo de configuración:**
```yaml
volumes:
  - ./plugin.properties:/etc/besu/plugin.properties
```

---

## Opción 2: Docker Individual

Si solo quieres el plugin compilado:

### Construir Imagen

```bash
docker build -t besu-permission-plugin:latest .
```

### Ejecutar Contenedor

```bash
docker run --name besu-permission \
  -v besu-plugins:/opt/besu/plugins \
  -v besu-logs:/var/log/besu \
  besu-permission-plugin:latest
```

### Verificar Plugin

```bash
docker run --rm -v besu-plugins:/plugins \
  alpine ls -la /plugins/
```

---

## Opción 3: Usar el Plugin con Besu en Docker

Si ya tienes Besu en Docker:

### 1. Compilar el plugin

```bash
./gradlew build
```

### 2. Montar el plugin en Besu

```bash
docker run \
  -p 8545:8545 \
  -p 30303:30303 \
  -p 9090:9090 \
  -v $(pwd)/build/libs/besu-permission-rpc-plugin-1.0.0-fat.jar:/opt/besu/plugins/besu-permission-rpc-plugin.jar \
  -v besu-data:/data \
  hyperledger/besu:latest \
    --data-path=/data \
    --p2p-port=30303 \
    --rpc-http-enabled \
    --rpc-http-port=8545 \
    --rpc-http-api=ETH,NET,WEB3,PERM \
    --plugin-dir=/opt/besu/plugins
```

---

## Ejemplo Completo: Stack de Monitoreo

### docker-compose.yml Extendido

```yaml
version: '3.8'

services:
  besu:
    image: hyperledger/besu:latest
    ports:
      - "8545:8545"
      - "30303:30303"
      - "9090:9090"
    volumes:
      - ./build/libs/besu-permission-rpc-plugin-1.0.0-fat.jar:/opt/besu/plugins/plugin.jar
      - besu-data:/data
      - besu-logs:/var/log/besu
    environment:
      - BESU_DATA_PATH=/data
      - BESU_RPC_HTTP_ENABLED=true
      - BESU_RPC_HTTP_PORT=8545
      - BESU_PLUGIN_DIR=/opt/besu/plugins
    command: >
      besu
      --data-path=/data
      --p2p-port=30303
      --rpc-http-enabled
      --rpc-http-port=8545
      --plugin-dir=/opt/besu/plugins
    networks:
      - monitoring

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9000:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - monitoring

  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    volumes:
      - ./loki.yml:/etc/loki/local-config.yml
      - loki-data:/loki
    networks:
      - monitoring

volumes:
  besu-data:
  besu-logs:
  prometheus-data:
  grafana-data:
  loki-data:

networks:
  monitoring:
```

### Iniciar Stack Completo

```bash
docker-compose up -d
```

### Acceder

- **Besu**: http://localhost:8545
- **Prometheus**: http://localhost:9000
- **Grafana**: http://localhost:3000
- **Loki**: http://localhost:3100

---

## Troubleshooting

### El plugin no se carga

```bash
# Ver logs
docker-compose logs besu | grep -i error

# Verificar que el archivo existe
docker exec besu-permission ls -la /opt/besu/plugins/
```

### Permisos denegados en volúmenes

```bash
# Dar permisos al directorio
sudo chmod -R 755 build/libs/

# O ejecutar con usuario específico
docker-compose exec -u root besu chmod 644 /opt/besu/plugins/*.jar
```

### Puerto ya en uso

Cambiar puertos en `docker-compose.yml`:

```yaml
ports:
  - "8547:8545"    # Cambiar a 8547
  - "9001:9090"    # Cambiar a 9001
```

### Espacio en disco bajo

```bash
# Limpiar volúmenes no utilizados
docker volume prune

# Limpiar imágenes no utilizadas
docker image prune -a
```

---

## Configuración de Producción

### Secrets en Docker

```yaml
services:
  besu:
    secrets:
      - webhook_url
    environment:
      - WEBHOOK_URL_FILE=/run/secrets/webhook_url

secrets:
  webhook_url:
    file: ./secrets/webhook_url.txt
```

### Recursos Limitados

```yaml
services:
  besu:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
```

### Health Checks

```yaml
services:
  besu:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8545"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

---

## Limpieza

```bash
# Detener y eliminar contenedores
docker-compose down

# Eliminar volúmenes
docker volume rm besu-permission-rpc-plugin_besu-data
docker volume rm besu-permission-rpc-plugin_besu-logs

# Eliminar imágenes
docker rmi besu-permission-plugin:latest
docker rmi hyperledger/besu:latest

# Limpieza completa
docker system prune -a --volumes
```

---

## Recursos

- [Docker Compose Docs](https://docs.docker.com/compose/)
- [Hyperledger Besu Docker](https://docs.besu.hyperledger.org/en/stable/HowTo/Get-Started/Run-Docker-Image/)
- [Prometheus Docker](https://hub.docker.com/r/prom/prometheus)
- [Grafana Docker](https://hub.docker.com/r/grafana/grafana)
