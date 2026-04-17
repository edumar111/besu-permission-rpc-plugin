# 🎯 Besu Permission RPC Plugin - Índice Completo

## 📦 Información del Proyecto

```
Nombre: besu-permission-rpc-plugin
Versión: 1.1.0
Lenguaje: Java 11+
Build: Gradle 8.5
Compatible: Hyperledger Besu 23.10.3+
Licencia: MIT
```

---

## 📥 DESCARGAS DISPONIBLES

### Opción 1: TAR.GZ (Linux/macOS) ⭐ RECOMENDADO
```
📦 besu-permission-rpc-plugin.tar.gz (32 KB)
```

**Instrucciones:**
```bash
# Descargar
wget https://tu-servidor/besu-permission-rpc-plugin.tar.gz

# Extraer
tar -xzf besu-permission-rpc-plugin.tar.gz

# Entrar
cd besu-permission-rpc-plugin

# Compilar y usar (ver QUICK_START.md)
./gradlew build
```

---

### Opción 2: ZIP (Windows/Todos)
```
📦 besu-permission-rpc-plugin.zip (50 KB)
```

**Instrucciones:**
```powershell
# Descargar
# Usar navegador web o:
# curl -O https://tu-servidor/besu-permission-rpc-plugin.zip

# Extraer (Windows Explorer o 7-Zip)
# Clic derecho > Extraer todo

# En PowerShell
cd besu-permission-rpc-plugin
.\gradlew.bat build
```

---

## 📚 DOCUMENTACIÓN INCLUIDA

### Para Empezar
- **[QUICK_START.md](./besu-permission-rpc-plugin/QUICK_START.md)** ⚡
  - Guía rápida (5 minutos)
  - Instalación paso a paso
  - Primeros pasos

### Documentación Completa
- **[README.md](./besu-permission-rpc-plugin/README.md)** 📖
  - Introducción y features
  - Requisitos
  - Instalación detallada
  - Configuración

- **[ADVANCED.md](./besu-permission-rpc-plugin/ADVANCED.md)** 🚀
  - REST API completa
  - Métricas Prometheus
  - Exportación de datos
  - Notificaciones y webhooks
  - Ejemplos prácticos

- **[DOCKER.md](./besu-permission-rpc-plugin/DOCKER.md)** 🐳
  - Docker individual
  - Docker Compose completo
  - Stack de monitoreo
  - Ejemplos de producción

- **[DOWNLOAD.md](./besu-permission-rpc-plugin/DOWNLOAD.md)** 📥
  - Instrucciones de descarga
  - Instalación por SO
  - Troubleshooting
  - Requisitos previos

---

## 🎯 CARACTERÍSTICAS

### ✅ Core del Plugin
- Interceptación automática de RPC permissions
- Captura de enode del nodo actual
- Captura de addresses/nodos permisionados
- Logging a archivo persistente
- Event listeners en tiempo real
- Soporte para múltiples tipos de eventos

### ✅ REST API (v1.1.0+)
```
GET  /api/v1/events                      # Todos los eventos
GET  /api/v1/events/type/{TYPE}          # Filtrar por tipo
GET  /api/v1/events/recent?hours=N       # Últimas N horas
GET  /api/v1/events/enode/{ENODE}        # Filtrar por enode
GET  /api/v1/stats                       # Estadísticas
GET  /api/v1/status                      # Estado del plugin
GET  /api/v1/events/export?format={FMT}  # Exportar (json,csv,xml,html)
```

### ✅ Métricas Prometheus
```
besu_permission_events_total
besu_permission_events_by_type{type="..."}
besu_permission_items_whitelisted
besu_permission_plugin_uptime_seconds
```

### ✅ Exportación Multi-formato
- JSON
- CSV
- XML
- TSV
- HTML (tabla)
- Reporte de texto

### ✅ Notificaciones
- Webhooks HTTP
- Integración Slack
- Integración Discord
- Payload personalizado

### ✅ Infraestructura
- Docker & Docker Compose
- Gradle Wrapper (sin instalar)
- Configuración flexible
- Servidor webhook incluido
- Documentación completa

---

## 📂 ESTRUCTURA DEL PROYECTO

```
besu-permission-rpc-plugin/
├── 📄 DOWNLOAD.md                    ← EMPIEZA AQUÍ
├── 📄 QUICK_START.md                 ← GUÍA RÁPIDA (5 min)
├── 📄 README.md                      ← Documentación principal
├── 📄 ADVANCED.md                    ← Features avanzadas
├── 📄 DOCKER.md                      ← Docker & Compose
├── 📄 INDEX.md                       ← Este archivo
│
├── build.gradle                      ← Configuración Gradle
├── settings.gradle
├── gradle/                           ← Gradle Wrapper
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── Dockerfile                        ← Para Docker
├── docker-compose.yml                ← Stack completo
├── prometheus.yml                    ← Prometheus config
├── webhook_server.py                 ← Servidor webhook (Python)
│
├── src/main/java/com/example/besu/plugin/
│   ├── PermissionInterceptorPlugin.java
│   ├── PermissionEventCapture.java
│   ├── PermissionEvent.java
│   ├── PermissionEventListener.java
│   ├── NodeInfoProvider.java
│   ├── PermissionRpcInterceptor.java
│   │
│   ├── api/
│   │   └── PermissionRestService.java
│   │
│   ├── metrics/
│   │   └── MetricsProvider.java
│   │
│   ├── export/
│   │   └── EventExporter.java
│   │
│   ├── config/
│   │   └── PluginConfig.java
│   │
│   ├── notifications/
│   │   └── EventNotifier.java
│   │
│   └── example/
│       └── PluginUsageExample.java
│
├── src/main/resources/
│   └── META-INF/services/
│       └── org.hyperledger.besu.plugin.BesuPlugin
│
├── plugin.properties.example         ← Configuración del plugin
├── besu.toml.example                 ← Ejemplo Besu config
├── install.sh                        ← Script de instalación
└── .gitignore
```

---

## 🚀 INSTALACIÓN RÁPIDA (3 PASOS)

### 1️⃣ Descargar y Extraer
```bash
# Linux/macOS
tar -xzf besu-permission-rpc-plugin.tar.gz
cd besu-permission-rpc-plugin

# Windows (PowerShell)
Expand-Archive besu-permission-rpc-plugin.zip
cd besu-permission-rpc-plugin
```

### 2️⃣ Compilar
```bash
# Automático (no necesita instalar Gradle)
./gradlew build
```

### 3️⃣ Instalar y Usar
```bash
# Copiar plugin
sudo cp build/libs/besu-permission-rpc-plugin-1.0.0-fat.jar /opt/besu/plugins/

# Iniciar Besu
besu --plugin-dir=/opt/besu/plugins \
     --rpc-http-enabled \
     --rpc-http-api=ETH,NET,WEB3,PERM
```

---

## 🎓 GUÍAS POR CASO DE USO

### 📱 "Quiero empezar en 5 minutos"
→ Lee **QUICK_START.md**

### 🔌 "Necesito usar la REST API"
→ Ve a **ADVANCED.md** → Sección "REST API"

### 📊 "Quiero monitorear con Prometheus/Grafana"
→ Ve a **ADVANCED.md** → Sección "Métricas Prometheus"

### 🐳 "Prefiero usar Docker"
→ Lee **DOCKER.md** → Sección "Docker Compose"

### 🔔 "Necesito notificaciones en Slack/Discord"
→ Ve a **ADVANCED.md** → Sección "Notificaciones"

### 📤 "Quiero exportar datos a CSV/JSON"
→ Ve a **ADVANCED.md** → Sección "Exportación de Datos"

### 🛠️ "Tengo problemas"
→ Ve a **DOWNLOAD.md** → Sección "Troubleshooting"

---

## ⚙️ REQUISITOS MÍNIMOS

### Sistema
- Linux, macOS o Windows (con WSL)
- 2 GB RAM libre mínimo
- 500 MB espacio en disco

### Software
- Java 11+ (verificar con `java -version`)
- Git (opcional, solo si clonas el repo)

### Para Besu
- Hyperledger Besu 23.10.3+
- Puerto 8545 disponible (RPC)
- Puerto 9090 disponible (Métricas, opcional)
- Puerto 30303 disponible (P2P)

---

## 🔧 COMANDOS ÚTILES

### Compilación
```bash
# Compilar todo
./gradlew build

# Compilar sin tests
./gradlew build -x test

# Solo crear JAR
./gradlew jar

# Limpiar previos
./gradlew clean
```

### Docker
```bash
# Compilar imagen
docker build -t besu-permission:latest .

# Docker Compose (todo incluido)
docker-compose up -d

# Ver logs
docker-compose logs -f besu

# Parar servicios
docker-compose down
```

### Testing
```bash
# Probar el plugin
curl -X POST http://127.0.0.1:8545/ \
  -d '{"jsonrpc":"2.0","method":"perm_addAccountsToAllowlist","params":[["0xaddress"]],"id":1}' \
  -H "Content-Type: application/json"

# Ver eventos
tail -f /var/log/besu/permission-events.log

# Obtener stats vía API
curl http://localhost:9091/api/v1/stats | jq
```

---

## 📞 SOPORTE & COMUNIDAD

### Documentación
- 📖 Besu Docs: https://besu.hyperledger.org
- 📖 Gradle Docs: https://gradle.org/guides
- 📖 Java Docs: https://docs.oracle.com/en/java/javase/11/

### Comunidad
- Hyperledger Besu: https://github.com/hyperledger/besu
- Stack Overflow: [hyperledger-besu]
- Discord: Hyperledger Besu

### Reporte de Bugs
Si encuentras un problema:
1. Revisa la sección "Troubleshooting" en DOWNLOAD.md
2. Verifica los logs: `tail -100 /opt/besu/logs/besu.log`
3. Intenta compilar nuevamente: `./gradlew clean build`

---

## 📊 ESTADÍSTICAS DEL PROYECTO

```
Archivos fuente Java:        9 clases
Clases de API:               1 clase
Clases de métricas:          1 clase
Clases de exportación:       1 clase
Clases de config:            1 clase
Clases de notificaciones:    1 clase
Total líneas de código:      ~1,500 LOC
Documentación:               6 archivos MD
Archivos de configuración:   5 ejemplos
Tamaño compilado:            ~500 KB
```

---

## 🗂️ TABLA DE CONTENIDOS RÁPIDA

| Documento | Para | Tiempo |
|-----------|------|--------|
| DOWNLOAD.md | Descargar e instalar | 5 min |
| QUICK_START.md | Primeros pasos | 10 min |
| README.md | Entender el proyecto | 15 min |
| ADVANCED.md | Funcionalidades avanzadas | 30 min |
| DOCKER.md | Docker & Compose | 20 min |

---

## ✨ VERSIONES

### v1.1.0 (Actual)
- ✅ REST API completa
- ✅ Métricas Prometheus
- ✅ Exportación multi-formato
- ✅ Notificaciones webhook
- ✅ Docker & Docker Compose
- ✅ Configuración flexible

### v1.0.0
- ✅ Core del plugin
- ✅ Interceptación de RPC
- ✅ Logging básico
- ✅ Event listeners

---

## 🎯 PRÓXIMO PASO

**→ Descarga uno de los archivos arriba y empieza con QUICK_START.md**

---

## 📜 LICENCIA

MIT License - Libre para usar, copiar y modificar

```
Copyright (c) 2024 Besu Permission RPC Plugin Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 🤝 CONTRIBUYE

¿Te gusta el proyecto? Considera:

1. ⭐ Darle una estrella en GitHub
2. 📢 Compartirlo con otros
3. 🐛 Reportar bugs
4. 💡 Sugerir features
5. 🔧 Hacer contribuciones

---

**Proyecto creado con ❤️ para Hyperledger Besu**

**Versión**: 1.1.0  
**Última actualización**: Enero 2024  
**Mantenedor**: Besu Community  
**Estado**: Production Ready ✅

---

## 🚀 ¡Listo para Comenzar!

### Próximos pasos:
1. ✅ Descarga uno de los archivos ZIP/TAR.GZ
2. ✅ Lee QUICK_START.md (5 minutos)
3. ✅ Compila el proyecto
4. ✅ Instala el plugin en Besu
5. ✅ ¡Pruébalo!

**¿Necesitas ayuda? Abre el archivo DOWNLOAD.md para troubleshooting.**
