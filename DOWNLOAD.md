# 📥 Descargar Besu Permission RPC Plugin

## 🔗 Links de Descarga

### Opción 1: TAR.GZ (Linux/macOS)
```
besu-permission-rpc-plugin.tar.gz (29 KB)
```

**Descargar desde este servidor:**
```bash
wget https://tu-servidor/besu-permission-rpc-plugin.tar.gz
```

**O desde Claude.ai:**
1. En la interfaz de Claude, busca el archivo `besu-permission-rpc-plugin.tar.gz`
2. Haz clic en el botón de descarga
3. Se descargará automáticamente

---

### Opción 2: ZIP (Windows/todos)
```
besu-permission-rpc-plugin.zip (46 KB)
```

**Descargar desde este servidor:**
```bash
wget https://tu-servidor/besu-permission-rpc-plugin.zip
```

**O desde Claude.ai:**
1. En la interfaz de Claude, busca el archivo `besu-permission-rpc-plugin.zip`
2. Haz clic en el botón de descarga
3. Se descargará automáticamente

---

## ⚡ Instalación Rápida Después de Descargar

### Linux/macOS

```bash
# 1. Extraer el archivo
tar -xzf besu-permission-rpc-plugin.tar.gz
cd besu-permission-rpc-plugin

# 2. Compilar (automático con Gradle Wrapper)
./gradlew clean build

# 3. Instalar
sudo mkdir -p /opt/besu/plugins /var/log/besu
sudo cp build/libs/besu-permission-rpc-plugin-1.0.0-fat.jar /opt/besu/plugins/
sudo chmod 755 /var/log/besu

# 4. Iniciar Besu
besu --data-path=/data \
     --rpc-http-enabled \
     --rpc-http-port=8545 \
     --rpc-http-api=ETH,NET,WEB3,PERM \
     --plugin-dir=/opt/besu/plugins

# 5. Probar en otra terminal
curl -X POST http://127.0.0.1:8545/ \
  -d '{"jsonrpc":"2.0","method":"perm_addAccountsToAllowlist","params":[["0xb9b81ee349c3807e46bc71aa2632203c5b462032"]],"id":1}' \
  -H "Content-Type: application/json"

# 6. Ver logs
tail -f /var/log/besu/permission-events.log
```

### Windows

```powershell
# 1. Extraer ZIP (usar Windows Explorer o 7-Zip)
# Clic derecho > Extraer todo

# 2. Abrir PowerShell en la carpeta extraída
cd besu-permission-rpc-plugin

# 3. Compilar
.\gradlew.bat clean build

# 4. Copiar JAR a plugins de Besu
Copy-Item "build\libs\besu-permission-rpc-plugin-1.0.0-fat.jar" "C:\Program Files\Besu\plugins\"

# 5. Iniciar Besu desde CMD
besu --data-path=C:\besu-data --rpc-http-enabled --rpc-http-port=8545 ^
     --rpc-http-api=ETH,NET,WEB3,PERM --plugin-dir="C:\Program Files\Besu\plugins"

# 6. Probar desde PowerShell
$headers = @{"Content-Type"="application/json"}
$body = '{"jsonrpc":"2.0","method":"perm_addAccountsToAllowlist","params":[["0xb9b81ee349c3807e46bc71aa2632203c5b462032"]],"id":1}'
Invoke-WebRequest -Uri "http://127.0.0.1:8545" -Method POST -Headers $headers -Body $body
```

### Docker

```bash
# 1. Extraer
tar -xzf besu-permission-rpc-plugin.tar.gz
cd besu-permission-rpc-plugin

# 2. Compilar
./gradlew build

# 3. Iniciar con Docker Compose (incluye Besu + Prometheus + Grafana)
docker-compose up -d

# 4. Verificar servicios
docker-compose ps

# 5. Acceder a:
# - Besu RPC: http://localhost:8545
# - Prometheus: http://localhost:9000
# - Grafana: http://localhost:3000 (user: admin, pass: admin)
# - Webhook: http://localhost:8000
```

---

## 📋 Requisitos Previos

### Linux/macOS

```bash
# Verificar Java
java -version
# Debe ser Java 11 o superior

# Si no tienes Java:
# Ubuntu/Debian
sudo apt-get install openjdk-11-jdk

# macOS
brew install openjdk@11

# CentOS/RHEL
sudo yum install java-11-openjdk
```

### Windows

- Descargar Java 11+ desde [java.com](https://www.java.com)
- Descargar Git Bash (opcional) desde [git-scm.com](https://git-scm.com)

### Docker (opcional)

- Docker 20.10+
- Docker Compose 2.0+

---

## 🎯 Qué hay dentro

```
besu-permission-rpc-plugin/
├── 📄 README.md                    # Documentación principal
├── 📄 QUICK_START.md              # Guía rápida (5 min)
├── 📄 ADVANCED.md                 # REST API, Prometheus, etc.
├── 📄 DOCKER.md                   # Docker y Docker Compose
├── build.gradle                   # Configuración Gradle
├── Dockerfile                     # Para Docker
├── docker-compose.yml             # Stack completo
├── plugin.properties.example       # Configuración del plugin
├── besu.toml.example              # Ejemplo Besu config
├── prometheus.yml                 # Prometheus config
├── webhook_server.py              # Servidor webhook en Python
├── gradle/                        # Gradle Wrapper
├── src/main/java/                 # Código fuente Java
│   └── com/example/besu/plugin/
│       ├── PermissionInterceptorPlugin.java
│       ├── PermissionEventCapture.java
│       ├── PermissionEvent.java
│       ├── PermissionEventListener.java
│       ├── NodeInfoProvider.java
│       ├── PermissionRpcInterceptor.java
│       ├── api/
│       │   └── PermissionRestService.java
│       ├── metrics/
│       │   └── MetricsProvider.java
│       ├── export/
│       │   └── EventExporter.java
│       ├── config/
│       │   └── PluginConfig.java
│       ├── notifications/
│       │   └── EventNotifier.java
│       └── example/
│           └── PluginUsageExample.java
└── .gitignore
```

---

## ✨ Características Incluidas

### v1.1.0 Completo

#### Core
- ✅ Interceptación automática de métodos RPC de permisos
- ✅ Captura de enode del nodo actual
- ✅ Captura de addresses/nodos permisionados
- ✅ Logging persistente a archivo
- ✅ Event listeners en tiempo real

#### API & Monitoreo
- ✅ REST API con múltiples endpoints
- ✅ Métricas Prometheus
- ✅ Exportación multi-formato (JSON, CSV, XML, TSV, HTML)
- ✅ Estadísticas en tiempo real
- ✅ Health checks

#### Notificaciones
- ✅ Webhooks HTTP
- ✅ Integración Slack
- ✅ Integración Discord
- ✅ Notificaciones personalizadas

#### Infraestructura
- ✅ Docker & Docker Compose
- ✅ Servidor webhook de ejemplo
- ✅ Gradle Wrapper (sin instalar Gradle)
- ✅ Configuración flexible
- ✅ Documentación completa

---

## 🚀 Primeros Pasos

### Paso 1: Descargar
Usa uno de los links arriba ⬆️

### Paso 2: Extraer
```bash
tar -xzf besu-permission-rpc-plugin.tar.gz
# o
unzip besu-permission-rpc-plugin.zip
```

### Paso 3: Compilar
```bash
cd besu-permission-rpc-plugin
./gradlew build
```

### Paso 4: Instalar
```bash
sudo cp build/libs/besu-permission-rpc-plugin-1.0.0-fat.jar /opt/besu/plugins/
```

### Paso 5: Usar
Lee **QUICK_START.md** para instrucciones completas

---

## 📚 Documentación

Después de descargar, abre estos archivos:

1. **QUICK_START.md** - Para empezar en 5 minutos
2. **README.md** - Para entender qué hace
3. **ADVANCED.md** - Para REST API, métricas, exportación
4. **DOCKER.md** - Para usar con Docker

---

## 🆘 Soporte & Ayuda

### Si tienes errores de compilación

```bash
# Limpiar y recompilar
./gradlew clean build --refresh-dependencies

# Ver más detalles
./gradlew build --info
```

### Si el plugin no se carga

```bash
# Verificar que está en el lugar correcto
ls -la /opt/besu/plugins/besu-permission-rpc-plugin-1.0.0-fat.jar

# Ver logs de Besu
tail -f /opt/besu/logs/besu.log | grep -i permission
```

### Si necesitas cambiar la configuración

Edita `plugin.properties.example` y cópialo como `plugin.properties`

```bash
cp plugin.properties.example plugin.properties
nano plugin.properties  # o tu editor favorito
```

---

## 💡 Próximos Pasos

Después de instalar:

1. ✅ Ejecuta `QUICK_START.md` para verificar que funciona
2. ✅ Lee `ADVANCED.md` para activar REST API y Prometheus
3. ✅ Usa `DOCKER.md` si quieres monitoreo completo
4. ✅ Configura notificaciones (webhooks, Slack, Discord)
5. ✅ Crea dashboards en Grafana

---

## 📊 Comparativa de Opciones de Instalación

| Opción | Ventajas | Desventajas | Para quién |
|--------|----------|-------------|-----------|
| **Instalación Manual** | Control total, lightweight | Más configuración | Usuarios avanzados |
| **Docker Individual** | Aislamiento, reproducible | Menos flexible | DevOps/Docker users |
| **Docker Compose** | Stack completo, monitoreo | Más recursos | Desarrollo/testing |

---

## 🎓 Recursos de Aprendizaje

### Besu
- [Documentación oficial](https://besu.hyperledger.org)
- [Plugins API](https://besu.hyperledger.org/en/stable/Concepts/Plugins/Plugins/)
- [RPC Methods](https://besu.hyperledger.org/en/stable/Reference/API-Methods/)

### Java
- [Java 11 Documentation](https://docs.oracle.com/en/java/javase/11/)
- [Gradle Guide](https://gradle.org/guides/)

### DevOps
- [Docker Docs](https://docs.docker.com/)
- [Prometheus Docs](https://prometheus.io/docs/)
- [Grafana Docs](https://grafana.com/docs/)

---

## 📜 Licencia

MIT License - Libre para usar, modificar y distribuir

---

## 🤝 Contribuciones

¿Encontraste un bug? ¿Tienes una idea?

1. Fork el repositorio
2. Crea una rama (`git checkout -b feature/AmazingFeature`)
3. Commit (`git commit -m 'Add AmazingFeature'`)
4. Push (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

**¡Listo para comenzar! 🚀**

Cualquier pregunta, revisa la documentación o abre un issue.

---

**Versión**: 1.1.0  
**Última actualización**: Enero 2024  
**Compatible con**: Hyperledger Besu 23.10.3+
