# Besu Permission RPC Plugin v2.0.0

Un plugin avanzado para Hyperledger Besu que intercepta llamadas RPC a métodos de permisos y captura automáticamente el enode del nodo y las direcciones/nodos permisionados.

> **✅ Actualizado para Besu 25.8.0 y Java 17** - Mejor rendimiento, más seguro, totalmente compatible.

## Características

✅ **Interceptación automática de RPC** - Captura eventos de `perm_addAccountsToAllowlist` y `perm_addNodesToAllowlist`
✅ **Extracción de datos automática** - Obtiene el enode del nodo actual y los items permisionados
✅ **Logging persistente** - Almacena eventos en `/var/log/besu/permission-events.log`
✅ **Event listeners** - Sistema de listeners para procesar eventos en tiempo real
✅ **Compatible con Gradle** - Compilación rápida y modular

## Requisitos

- Java 17+ (antes era 11+)
- Gradle 7.0+
- Hyperledger Besu 25.8.0+ (antes era 23.10.3+)

## Estructura del Proyecto

```
besu-permission-rpc-plugin/
├── build.gradle                                          # Configuración Gradle
├── settings.gradle                                       # Configuración del proyecto
├── README.md                                             # Este archivo
├── src/
│   └── main/
│       ├── java/com/example/besu/plugin/
│       │   ├── PermissionInterceptorPlugin.java         # Plugin principal
│       │   ├── PermissionEventCapture.java              # Capturador de eventos
│       │   ├── PermissionEvent.java                     # Modelo de evento
│       │   ├── PermissionEventListener.java             # Interfaz de listener
│       │   ├── NodeInfoProvider.java                    # Proveedor de info del nodo
│       │   └── PermissionRpcInterceptor.java            # Interceptor RPC
│       └── resources/
│           └── META-INF/services/
│               └── org.hyperledger.besu.plugin.BesuPlugin
└── build/                                                # Salida compilada
    ├── libs/
    │   ├── besu-permission-rpc-plugin-1.0.0.jar
    │   └── besu-permission-rpc-plugin-1.0.0-fat.jar
```

## Compilación

### Opción 1: Compilar con Gradle

```bash
# Clonar el repositorio
git clone <tu-repositorio>
cd besu-permission-rpc-plugin

# Compilar el proyecto
./gradlew build

# El JAR compilado estará en:
# build/libs/besu-permission-rpc-plugin-1.0.0-fat.jar
```

### Opción 2: Compilación rápida

```bash
./gradlew jar
```

## Instalación en Besu

### 1. Crear directorio de plugins

```bash
mkdir -p /opt/besu/plugins
```

### 2. Copiar el JAR compilado

```bash
cp build/libs/besu-permission-rpc-plugin-1.0.0-fat.jar /opt/besu/plugins/
```

### 3. Crear directorio de logs

```bash
mkdir -p /var/log/besu
chmod 755 /var/log/besu
```

### 4. Iniciar Besu con el plugin

```bash
besu \
  --data-path=/data \
  --p2p-port=30303 \
  --rpc-http-enabled \
  --rpc-http-port=8545 \
  --rpc-http-api=ETH,NET,WEB3,PERM \
  --plugin-dir=/opt/besu/plugins
```

## Configuración en besu.toml

```toml
[Node]
data-path = "/data"
p2p-port = 30303
plugin-dir = "/opt/besu/plugins"

[RPC]
http-enabled = true
http-port = 8545
http-api = ["ETH", "NET", "WEB3", "PERM"]
```

## Uso

### Ejemplo 1: Agregar cuentas al allowlist

```bash
curl -X POST \
  --data '{"jsonrpc":"2.0","method":"perm_addAccountsToAllowlist","params":[["0xb9b81ee349c3807e46bc71aa2632203c5b462032", "0xb9b81ee349c3807e46bc71aa2632203c5b462034"]],"id":1}' \
  http://127.0.0.1:8545/ \
  -H "Content-Type: application/json"
```

**Salida esperada en consola:**

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

### Ejemplo 2: Ver los logs

```bash
tail -f /var/log/besu/permission-events.log
```

**Formato del log:**

```
[2024-01-15 14:23:45.123] TYPE=ADD_ACCOUNTS | ENODE=enode://a1b2c3d4e5f6...@127.0.0.1:30303 | ITEMS=0xb9b81ee349c3807e46bc71aa2632203c5b462032,0xb9b81ee349c3807e46bc71aa2632203c5b462034
[2024-01-15 14:24:10.456] TYPE=ADD_NODES | ENODE=enode://a1b2c3d4e5f6...@127.0.0.1:30303 | ITEMS=enode://node1@192.168.1.100:30303,enode://node2@192.168.1.101:30303
```

## API del Plugin

### PermissionEventCapture

```java
// Obtener todos los eventos capturados
List<PermissionEvent> events = eventCapture.getEvents();

// Filtrar por tipo
List<PermissionEvent> accountEvents = eventCapture.getEventsByType(
    PermissionEvent.EventType.ADD_ACCOUNTS
);

// Obtener conteo total
int total = eventCapture.getTotalEventsCount();

// Limpiar eventos
eventCapture.clearEvents();

// Agregar listeners
eventCapture.addListener(event -> {
    System.out.println("Evento: " + event);
});
```

### PermissionEvent

```java
// Obtener información del evento
String enode = event.getEnode();
List<String> items = event.getAddresses();
LocalDateTime timestamp = event.getTimestamp();
PermissionEvent.EventType type = event.getEventType();

// Tipos disponibles
// - ADD_ACCOUNTS (perm_addAccountsToAllowlist)
// - ADD_NODES (perm_addNodesToAllowlist)
// - REMOVE_ACCOUNTS (perm_removeAccountsFromAllowlist)
// - REMOVE_NODES (perm_removeNodesFromAllowlist)
```

## Troubleshooting

### El plugin no se carga

1. Verifica que el archivo JAR esté en `/opt/besu/plugins/`
2. Revisa los logs de Besu: `tail -f /opt/besu/logs/besu.log`
3. Asegúrate de usar una versión compatible de Besu (23.10.3+)

### No se crean logs

```bash
# Verifica que el directorio existe y tiene permisos
ls -la /var/log/besu/
chmod 755 /var/log/besu

# Reinicia Besu
```

### El enode dice "unknown"

Esto es normal si el nodo no ha finalizado el bootstrap de P2P. El enode se actualizará cuando el nodo esté completamente inicializado.

## Desarrollo

### Agregar un nuevo tipo de evento

1. Agrega un nuevo tipo en `PermissionEvent.EventType`
2. Crea un nuevo método en `PermissionEventCapture.java`
3. Actualiza el interceptor en `PermissionRpcInterceptor.java`

### Compilar con debug

```bash
./gradlew build --debug
```

### Ejecutar pruebas unitarias

```bash
./gradlew test
```

## Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el repositorio
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Licencia

MIT License - Ver LICENSE para detalles

## Soporte

Para reportar bugs o solicitar features, abre un issue en GitHub.

## Versión

**v2.0.0** - Enero 2025

- ✅ Actualizado para Besu 25.8.0
- ✅ Soporte para Java 17
- ✅ Dependencias actualizadas (Jackson 2.17.0, Log4j 2.23.1, HttpClient 5.3.1)
- ✅ Mejor rendimiento y seguridad
- ✅ 100% compatible con v1.1.0

---

**Desarrollado para Hyperledger Besu 23.10.3+**
