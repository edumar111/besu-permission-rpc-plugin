# 🚀 Guía de Inicio Rápido - Besu Permission RPC Plugin v2.0.0

> **✨ ACTUALIZADO para Besu 25.8.0 y Java 17**

## Descarga e Instalación Rápida (5 minutos)

### 1️⃣ Descargar el proyecto

```bash
# Opción A: Descargar TAR.GZ
wget https://tu-url/besu-permission-rpc-plugin.tar.gz
tar -xzf besu-permission-rpc-plugin.tar.gz
cd besu-permission-rpc-plugin

# Opción B: Descargar ZIP
wget https://tu-url/besu-permission-rpc-plugin.zip
unzip besu-permission-rpc-plugin.zip
cd besu-permission-rpc-plugin
```

### 2️⃣ Compilar el proyecto

```bash
# Opción A: Script de instalación automática (recomendado)
chmod +x install.sh
sudo ./install.sh

# Opción B: Compilación manual
./gradlew clean build
```

### 3️⃣ Verificar la compilación

```bash
# Deberías ver:
# build/libs/besu-permission-rpc-plugin-2.0.0.jar
# build/libs/besu-permission-rpc-plugin-2.0.0-fat.jar

ls -lh build/libs/
```

### 4️⃣ Instalar manualmente (si no usaste el script)

```bash
# Crear directorios
sudo mkdir -p /opt/besu/plugins
sudo mkdir -p /var/log/besu
sudo chmod 755 /var/log/besu

# Copiar el JAR compilado
sudo cp build/libs/besu-permission-rpc-plugin-2.0.0-fat.jar \
  /opt/besu/plugins/
```

### 5️⃣ Iniciar Besu con el plugin

```bash
besu \
  --data-path=/data \
  --p2p-port=30303 \
  --rpc-http-enabled \
  --rpc-http-port=8545 \
  --rpc-http-api=ETH,NET,WEB3,PERM \
  --plugin-dir=/opt/besu/plugins
```

### 6️⃣ Verificar que el plugin se cargó

En la salida de Besu deberías ver:

```
════════════════════════════════════════════════════════════════════════════════
[BESU PERMISSION RPC PLUGIN] Registrando plugin...
════════════════════════════════════════════════════════════════════════════════

✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓
[✓] PermissionInterceptor Plugin iniciado correctamente
[✓] Monitoreando: perm_addAccountsToAllowlist
[✓] Monitoreando: perm_addNodesToAllowlist
[✓] Registros en: /var/log/besu/permission-events.log
✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓
```

### 7️⃣ Prueba del plugin

```bash
# Ejecutar desde otra terminal
curl -X POST \
  --data '{"jsonrpc":"2.0","method":"perm_addAccountsToAllowlist","params":[["0xb9b81ee349c3807e46bc71aa2632203c5b462032", "0xb9b81ee349c3807e46bc71aa2632203c5b462034"]],"id":1}' \
  http://127.0.0.1:8545/ \
  -H "Content-Type: application/json"
```

En la consola de Besu deberías ver:

```
════════════════════════════════════════════════════════════════════════════════════════════════════════════════
┌─ [PERMISSION EVENT INTERCEPTED] 2024-01-15 14:23:45.123
├─ TYPE: ADD_ACCOUNTS
├─ ENODE (Current Node): enode://a1b2c3d4e5f6...@127.0.0.1:30303
├─ ITEMS ADDED: 2
├─  [1] 0xb9b81ee349c3807e46bc71aa2632203c5b462032
└─  [2] 0xb9b81ee349c3807e46bc71aa2632203c5b462034
└─ TIMESTAMP: 2024-01-15 14:23:45.123
════════════════════════════════════════════════════════════════════════════════════════════════════════════════
```
```
curl -X POST \
--data '{"jsonrpc":"2.0","method":"perm_addAccountsToAllowlist","params":[["0x0561D5c2f2FfC3544B68a116a2F4510B1eeF03fc"]],"id":1}' \
http://127.0.0.1:4545/ \
-H "Content-Type: application/json"
```

```
curl -X POST \
--data '{"jsonrpc":"2.0","method":"perm_removeAccountsFromAllowlist","params":[["0x0561D5c2f2FfC3544B68a116a2F4510B1eeF03fc"]],"id":1}' \
http://127.0.0.1:4545/ \
-H "Content-Type: application/json"
```
### 8️⃣ Ver los logs

```bash
# Ver logs en tiempo real
tail -f /var/log/besu/permission-events.log

# Ver últimas 10 líneas
tail -10 /var/log/besu/permission-events.log

# Buscar eventos específicos
grep "ADD_ACCOUNTS" /var/log/besu/permission-events.log
```

---

## 🛠️ Estructura del Proyecto

```
besu-permission-rpc-plugin/
├── build.gradle                              # Configuración Gradle
├── settings.gradle                           # Configuración del proyecto
├── gradle/                                   # Gradle wrapper
├── src/
│   └── main/
│       ├── java/com/example/besu/plugin/    # Código fuente
│       │   ├── PermissionInterceptorPlugin.java
│       │   ├── PermissionEventCapture.java
│       │   ├── PermissionEvent.java
│       │   ├── PermissionEventListener.java
│       │   ├── NodeInfoProvider.java
│       │   └── PermissionRpcInterceptor.java
│       └── resources/META-INF/services/     # Config de servicios
├── README.md                                 # Documentación completa
├── besu.toml.example                        # Ejemplo de configuración
├── install.sh                               # Script de instalación
└── QUICK_START.md                           # Este archivo
```

---

## ⚡ Comandos Útiles

```bash
# Compilar el proyecto
./gradlew build

# Compilar sin ejecutar tests
./gradlew build -x test

# Compilación rápida (solo JAR)
./gradlew jar

# Limpiar build previos
./gradlew clean

# Ver información del build
./gradlew properties

# Ejecutar con más detalles
./gradlew build --info
```

---

## 📋 Requisitos

- ✅ Java 11+
- ✅ Gradle 7.0+ (incluido en el proyecto)
- ✅ Hyperledger Besu 23.10.3+
- ✅ Linux/macOS/Windows (con WSL)

---

## 🔍 Troubleshooting

### ❌ Error: "permission denied" en install.sh

```bash
chmod +x install.sh
```

### ❌ Error: "Java not found"

```bash
# Verificar que Java está instalado
java -version

# Si no aparece, instálalo:
# Ubuntu/Debian
sudo apt-get install openjdk-11-jdk

# macOS
brew install openjdk@11

# CentOS/RHEL
sudo yum install java-11-openjdk
```

### ❌ El plugin no se carga en Besu

1. Verifica que el archivo está en el lugar correcto:
```bash
ls -la /opt/besu/plugins/besu-permission-rpc-plugin-1.0.0-fat.jar
```

2. Revisa los logs de Besu:
```bash
tail -100 /opt/besu/logs/besu.log
```

3. Asegúrate de que la versión de Besu es compatible (23.10.3+)

### ❌ Los logs no se crean

```bash
# Verifica que el directorio existe
sudo mkdir -p /var/log/besu
sudo chmod 755 /var/log/besu

# Reinicia Besu
```

### ❌ El enode dice "unknown"

Es normal. El enode se actualiza cuando el nodo completa el bootstrap de P2P.

---

## 📚 Documentación Completa

Para más detalles, consulta:

- `README.md` - Documentación completa
- `besu.toml.example` - Configuración de ejemplo
- Código fuente con comentarios en:
  - `src/main/java/com/example/besu/plugin/`

---

## 📞 Soporte

Para problemas o sugerencias:

1. Revisa el README.md
2. Verifica los logs en `/var/log/besu/permission-events.log`
3. Abre un issue en GitHub

---

**¡Listo para usar! 🚀**
