# 📚 Documentación Avanzada - Besu Permission RPC Plugin

## 📋 Tabla de Contenidos

1. [REST API](#rest-api)
2. [Métricas Prometheus](#métricas-prometheus)
3. [Exportación de Datos](#exportación-de-datos)
4. [Configuración](#configuración)
5. [Notificaciones](#notificaciones)
6. [Ejemplos Prácticos](#ejemplos-prácticos)

---

## REST API

### Endpoints Disponibles

#### 1. Obtener todos los eventos

```bash
GET /api/v1/events
```

**Respuesta:**
```json
{
  "success": true,
  "total": 5,
  "events": [
    {
      "type": "ADD_ACCOUNTS",
      "method": "perm_addAccountsToAllowlist",
      "enode": "enode://a1b2c3d4e5f6...@127.0.0.1:30303",
      "items": ["0xaddress1", "0xaddress2"],
      "items_count": 2,
      "timestamp": "2024-01-15 14:23:45.123"
    }
  ]
}
```

#### 2. Filtrar por tipo de evento

```bash
GET /api/v1/events/type/ADD_ACCOUNTS
GET /api/v1/events/type/ADD_NODES
GET /api/v1/events/type/REMOVE_ACCOUNTS
GET /api/v1/events/type/REMOVE_NODES
```

**Respuesta:**
```json
{
  "success": true,
  "type": "ADD_ACCOUNTS",
  "total": 3,
  "events": [...]
}
```

#### 3. Obtener eventos recientes

```bash
GET /api/v1/events/recent?hours=1
GET /api/v1/events/recent?hours=24
```

**Parámetros:**
- `hours` (int): Número de horas hacia atrás

**Respuesta:**
```json
{
  "success": true,
  "hours": 1,
  "since": "2024-01-15 13:23:45",
  "total": 2,
  "events": [...]
}
```

#### 4. Filtrar por enode

```bash
GET /api/v1/events/enode/enode://a1b2c3d4e5f6...@127.0.0.1:30303
```

#### 5. Obtener estadísticas

```bash
GET /api/v1/stats
```

**Respuesta:**
```json
{
  "success": true,
  "total_events": 15,
  "add_accounts_events": 10,
  "add_nodes_events": 5,
  "unique_enodes": 2,
  "total_items_whitelisted": 35
}
```

#### 6. Obtener estado del plugin

```bash
GET /api/v1/status
```

**Respuesta:**
```json
{
  "success": true,
  "plugin_status": "ACTIVE",
  "total_events_captured": 42,
  "log_file": "/var/log/besu/permission-events.log",
  "version": "1.0.0"
}
```

---

## Métricas Prometheus

### Acceso a Métricas

```bash
curl http://localhost:9090/metrics
```

### Métricas Disponibles

```
# Total de eventos capturados
besu_permission_events_total 42

# Eventos por tipo
besu_permission_events_by_type{type="ADD_ACCOUNTS"} 30
besu_permission_events_by_type{type="ADD_NODES"} 12
besu_permission_events_by_type{type="REMOVE_ACCOUNTS"} 0
besu_permission_events_by_type{type="REMOVE_NODES"} 0

# Items en whitelist
besu_permission_items_whitelisted 85

# Uptime del plugin
besu_permission_plugin_uptime_seconds 3600
```

### Configurar Prometheus

Edita `/etc/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'besu-permission'
    static_configs:
      - targets: ['localhost:9090']
```

### Visualizar en Grafana

1. Conectar Prometheus como datasource
2. Crear dashboard con:
   - `besu_permission_events_total` (contador)
   - `besu_permission_events_by_type` (gauge por tipo)
   - `besu_permission_items_whitelisted` (gauge)
   - `besu_permission_plugin_uptime_seconds` (uptime)

---

## Exportación de Datos

### Formatos Soportados

#### 1. JSON

```bash
curl http://localhost:9091/api/v1/events/export?format=json > events.json
```

**Ejemplo:**
```json
[
  {
    "type": "ADD_ACCOUNTS",
    "enode": "enode://...",
    "addresses": ["0xaddr1", "0xaddr2"],
    "timestamp": "2024-01-15T14:23:45"
  }
]
```

#### 2. CSV

```bash
curl http://localhost:9091/api/v1/events/export?format=csv > events.csv
```

**Formato:**
```csv
TYPE,ENODE,ITEMS,TIMESTAMP
ADD_ACCOUNTS,enode://...,0xaddr1|0xaddr2,2024-01-15 14:23:45
```

#### 3. XML

```bash
curl http://localhost:9091/api/v1/events/export?format=xml > events.xml
```

#### 4. TSV (Tab-Separated Values)

```bash
curl http://localhost:9091/api/v1/events/export?format=tsv > events.tsv
```

#### 5. HTML (Tabla)

```bash
curl http://localhost:9091/api/v1/events/export?format=html > report.html
```

#### 6. Reporte de Resumen

```bash
curl http://localhost:9091/api/v1/events/export?format=summary > report.txt
```

---

## Configuración

### Opción 1: Via Archivo de Propiedades

Copia el archivo de configuración:

```bash
cp plugin.properties.example /etc/besu/plugin.properties
```

Edita `/etc/besu/plugin.properties`:

```properties
# Logging
log.file=/var/log/besu/permission-events.log
log.level=INFO

# API REST
api.rest.enabled=true
api.rest.port=9091

# Métricas
metrics.enabled=true
metrics.port=9090

# Memoria
memory.max.events=10000

# Notificaciones
notification.webhook=http://localhost:8000/webhook
```

Inicia Besu con la configuración:

```bash
besu \
  --data-path=/data \
  --rpc-http-enabled \
  --plugin-dir=/opt/besu/plugins \
  -Dbesu.permission.config=/etc/besu/plugin.properties
```

### Opción 2: Via Propiedades del Sistema

```bash
besu \
  --data-path=/data \
  --rpc-http-enabled \
  --plugin-dir=/opt/besu/plugins \
  -Dbesu.permission.log.file=/custom/path.log \
  -Dbesu.permission.metrics.enabled=true \
  -Dbesu.permission.metrics.port=9090 \
  -Dbesu.permission.api.rest.enabled=true
```

### Opción 3: Via Variables de Entorno

```bash
export BESU_PERMISSION_LOG_FILE=/var/log/besu/events.log
export BESU_PERMISSION_METRICS_PORT=9090
export BESU_PERMISSION_API_REST_ENABLED=true

besu --data-path=/data --rpc-http-enabled --plugin-dir=/opt/besu/plugins
```

---

## Notificaciones

### Tipos de Webhook Soportados

#### 1. Webhook Genérico (HTTP)

```properties
notification.webhook=http://localhost:8000/webhook
```

**Payload enviado:**
```json
{
  "event_type": "ADD_ACCOUNTS",
  "method": "perm_addAccountsToAllowlist",
  "enode": "enode://...",
  "items": ["0xaddr1"],
  "items_count": 1,
  "timestamp": "2024-01-15T14:23:45"
}
```

#### 2. Slack

```properties
notification.webhook=https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK
```

**Resultado en Slack:**
```
[Green] Permission Event: ADD_ACCOUNTS
Type: ADD_ACCOUNTS
Enode: enode://a1b2c3...@127.0.0.1:30303
Items Count: 2
Items: 0xaddr1, 0xaddr2
Timestamp: 2024-01-15 14:23:45
```

#### 3. Discord

```properties
notification.webhook=https://discordapp.com/api/webhooks/YOUR/DISCORD/WEBHOOK
```

#### 4. Microsoft Teams

```properties
notification.webhook=https://outlook.webhook.office.com/webhookb2/YOUR/TEAMS/WEBHOOK
```

### Ejemplo: Servidor Webhook Local

```python
from flask import Flask, request
import json

app = Flask(__name__)

@app.route('/webhook', methods=['POST'])
def webhook():
    data = request.json
    print(f"Event Type: {data['event_type']}")
    print(f"Enode: {data['enode']}")
    print(f"Items: {data['items']}")
    
    # Aquí puedes procesar el evento
    # Guardar en BD, enviar email, etc.
    
    return {'status': 'ok'}, 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
```

---

## Ejemplos Prácticos

### Ejemplo 1: Monitoreo con Prometheus + Grafana

1. **Instalar Prometheus:**
```bash
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz
tar xzf prometheus-2.45.0.linux-amd64.tar.gz
cd prometheus-2.45.0.linux-amd64
```

2. **Configurar Prometheus (`prometheus.yml`):**
```yaml
scrape_configs:
  - job_name: 'besu-permission'
    static_configs:
      - targets: ['localhost:9090']
    scrape_interval: 5s
```

3. **Iniciar Prometheus:**
```bash
./prometheus --config.file=prometheus.yml
```

4. **Ver métricas en:**
```
http://localhost:9090
```

### Ejemplo 2: Alertas con Slack

1. **Configurar webhook en Slack:**
   - Ve a tu workspace de Slack
   - Settings > Apps > Crear nueva app
   - Activa "Incoming Webhooks"
   - Copia el webhook URL

2. **Configurar en plugin:**
```properties
notification.webhook=https://hooks.slack.com/services/YOUR_WORKSPACE/YOUR_CHANNEL/YOUR_TOKEN
```

3. **Iniciar Besu y probar:**
```bash
besu --plugin-dir=/opt/besu/plugins \
     -Dbesu.permission.config=/etc/besu/plugin.properties
```

### Ejemplo 3: Exportar a Base de Datos

```python
import requests
import sqlite3

# Obtener eventos
response = requests.get('http://localhost:9091/api/v1/events')
events = response.json()['events']

# Guardar en SQLite
conn = sqlite3.connect('permission_events.db')
cursor = conn.cursor()

cursor.execute('''
    CREATE TABLE IF NOT EXISTS events (
        id INTEGER PRIMARY KEY,
        type TEXT,
        enode TEXT,
        items TEXT,
        timestamp TEXT
    )
''')

for event in events:
    cursor.execute('''
        INSERT INTO events (type, enode, items, timestamp)
        VALUES (?, ?, ?, ?)
    ''', (
        event['type'],
        event['enode'],
        ','.join(event['items']),
        event['timestamp']
    ))

conn.commit()
conn.close()

print(f"Guardados {len(events)} eventos en la BD")
```

### Ejemplo 4: Dashboard Web Personalizado

```html
<!DOCTYPE html>
<html>
<head>
    <title>Permission Events Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <h1>Permission Events Dashboard</h1>
    
    <div id="stats"></div>
    <canvas id="chart"></canvas>
    
    <script>
        // Obtener estadísticas
        fetch('http://localhost:9091/api/v1/stats')
            .then(r => r.json())
            .then(data => {
                document.getElementById('stats').innerHTML = `
                    <p>Total Events: ${data.total_events}</p>
                    <p>Add Accounts: ${data.add_accounts_events}</p>
                    <p>Add Nodes: ${data.add_nodes_events}</p>
                `;
                
                // Crear gráfico
                new Chart(document.getElementById('chart'), {
                    type: 'doughnut',
                    data: {
                        labels: ['Add Accounts', 'Add Nodes'],
                        datasets: [{
                            data: [
                                data.add_accounts_events,
                                data.add_nodes_events
                            ],
                            backgroundColor: ['#36a64f', '#0099cc']
                        }]
                    }
                });
            });
    </script>
</body>
</html>
```

---

## Preguntas Frecuentes

**¿Cómo cambio el puerto de las métricas?**
```bash
-Dbesu.permission.metrics.port=9999
```

**¿Dónde se guardan los eventos?**
En memoria y en `/var/log/besu/permission-events.log`

**¿Puedo exportar datos históricos?**
Sí, usa los endpoints `/export?format=csv`, `/export?format=json`, etc.

**¿Funciona con Besu en Docker?**
Sí, monta el directorio `/opt/besu/plugins`

---

## Versión

**v1.1.0** - Enero 2024

- ✅ REST API completa
- ✅ Métricas Prometheus
- ✅ Exportación multi-formato
- ✅ Sistema de notificaciones
- ✅ Configuración flexible

