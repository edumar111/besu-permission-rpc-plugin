# Besu Permission RPC Plugin

Plugin para Hyperledger Besu que detecta cambios en la allowlist de cuentas y nodos (`perm_addAccountsToAllowlist`, `perm_removeAccountsFromAllowlist`, `perm_addNodesToAllowlist`, `perm_removeNodesFromAllowlist`), los registra en un log local y los reporta on-chain a un contrato `PermissionAuditLog` firmando las transacciones con la llave privada del nodo.

- **Versión:** 2.0.0
- **Besu compatible:** 25.8.0+
- **Java:** 21 (toolchain Gradle)

## Cómo funciona

El plugin **no** intercepta el RPC directamente. Observa el fichero `permissions_config.toml` (el que Besu modifica cuando recibe una llamada `perm_*`) con un `WatchService`, calcula el diff entre el estado anterior y el nuevo, y por cada alta/baja:

1. Añade el evento a un store en memoria (`CopyOnWriteArrayList`).
2. Escribe un bloque formateado al log del plugin (por defecto `/var/log/besu/permission-events.log`).
3. Si `blockchain.enabled=true`, encola una tx firmada para llamar a `reportAccountAdded/Removed` o `reportEnodeAdded/Removed` en el contrato `PermissionAuditLog`, usando la `key` del nodo como signer.

```
Besu escribe permissions_config.toml
  └─ file watcher calcula diff → PermissionEventCapture.capture*
       ├─ añade PermissionEvent al store
       ├─ escribe bloque al log del plugin
       └─ notifica listeners → BlockchainReporter
            └─ firma tx + eth_sendRawTransaction → PermissionAuditLog.reportXxx(...)
```

## Requisitos

- **Java 21** (Gradle toolchain la descarga automáticamente si no la tienes).
- **Hyperledger Besu 25.8.0** o superior corriendo con `--rpc-http-enabled` y al menos la API `PERM` habilitada (más `ETH,NET,WEB3`).
- Una dirección de contrato `PermissionAuditLog` desplegada en la misma red, y la `key` privada del nodo (hex de 32 bytes) disponible en disco — que es la cuenta que firmará las tx.
- Esa misma cuenta **debe estar en la allowlist de accounts** si tu red tiene account-permissioning activado; si no, Besu rechazará sus tx con `-32007 "Sender account not authorized to send transactions"`.

## Compilación

```bash
./gradlew clean fatJar
ls build/libs/
# → besu-permission-rpc-plugin-2.0.0-fat.jar
```

El artefacto desplegable es el **fat JAR**. El `jar` "finito" (sin dependencias) no sirve para Besu porque faltarían Jackson, HttpClient y web3j en runtime.

Tareas Gradle útiles:

```bash
./gradlew build              # compila + tests + fatJar
./gradlew build -x test      # compila + fatJar sin tests
./gradlew fatJar             # solo el fat JAR
./gradlew clean              # limpia build/
./gradlew test               # solo tests
```

## Instalación

1. Copia el fat JAR al directorio de plugins de Besu:

    ```bash
    cp build/libs/besu-permission-rpc-plugin-2.0.0-fat.jar /opt/besu/plugins/
    ```

2. Crea el fichero de configuración del plugin (ver [Configuración](#configuración)) y pásalo a Besu por `-D`:

    ```bash
    besu \
      --data-path=/data \
      --p2p-port=30303 \
      --rpc-http-enabled \
      --rpc-http-port=8545 \
      --rpc-http-api=ETH,NET,WEB3,PERM \
      --plugin-dir=/opt/besu/plugins \
      -Dbesu.permission.config=/etc/besu/plugin.properties
    ```

3. Arranca Besu. En el log del plugin verás el banner de arranque con `Log File`, `Max Events in Memory`, `Local Node Enode: resolving...` y, si `blockchain.enabled=true`, `[BLOCKCHAIN] Reporter started. Sender address: 0x…`.

## Configuración

El plugin lee sus propiedades desde el fichero apuntado por `-Dbesu.permission.config=...` o (si no se define) desde propiedades del sistema `-Dbesu.permission.<clave>=...`.

Plantilla completa en [`plugin.properties.example`](./plugin.properties.example). Opciones:

| Clave | Sistema property | Default | Descripción |
|---|---|---|---|
| `log.file` | `besu.permission.log.file` | `/var/log/besu/permission-events.log` (Linux) | Fichero donde se escriben los bloques de eventos |
| `memory.max.events` | `besu.permission.memory.max.events` | `10000` | Máximo de eventos mantenidos en memoria |
| `data.path` | `besu.permission.data.path` | resuelto vía `BesuConfiguration` | Data path de Besu (donde está `permissions_config.toml`) |
| `rpc.port` | `besu.permission.rpc.port` | resuelto vía `BesuConfiguration` | Puerto HTTP RPC local, usado para resolver el enode y enviar tx |
| `rpc.host` | `besu.permission.rpc.host` | `127.0.0.1` | — |
| `blockchain.enabled` | `besu.permission.blockchain.enabled` | `false` | Activa el reporte on-chain |
| `blockchain.contract.address` | `besu.permission.blockchain.contract.address` | — | Address del `PermissionAuditLog` |
| `blockchain.chain.id` | `besu.permission.blockchain.chain.id` | — | Chain ID (EIP-155) |
| `blockchain.node.key.path` | `besu.permission.blockchain.node.key.path` | — | Ruta a la key privada del nodo (relativa al data-path o absoluta) |
| `blockchain.rpc.url` | `besu.permission.blockchain.rpc.url` | `http://127.0.0.1:${rpc.port}/` | RPC donde se envían las tx |
| `blockchain.gas.price` | `besu.permission.blockchain.gas.price` | — | gasPrice en wei (0 en redes sin fees) |
| `blockchain.gas.limit` | `besu.permission.blockchain.gas.limit` | — | gasLimit por tx |

Si `blockchain.enabled=true` y falta alguna de `contract.address`, `chain.id`, `node.key.path`, `gas.limit`, el plugin imprime `[BLOCKCHAIN ERROR] Propiedades requeridas faltantes en plugin.properties: [...]` y no arranca el reporter (el watcher y el log local sí siguen funcionando).

## Verificar que funciona

Tras arrancar Besu, ejecuta una alta de cuenta:

```bash
curl -s -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"perm_addAccountsToAllowlist","params":[["0x99590c4D8971275e6D90467724D9169Ab7d1749F"]],"id":1}' \
  http://127.0.0.1:8545/
```

1. **Log del plugin** (`permission-events.log`) — deberías ver:

    ```
    ════════════════════════════════════════════════════════════════════════════════════════════════════
    ┌─ [PERMISSION EVENT INTERCEPTED] 2026-04-23 20:46:11.101
    ├─ TYPE: ADD_ACCOUNTS
    ├─ ENODE (Current Node): enode://…@<ip>:<port>
    ├─ ITEMS ADDED: 1
    └─  [1] 0x99590c4d8971275e6d90467724d9169ab7d1749f
    └─ TIMESTAMP: 2026-04-23 20:46:11.101
    ════════════════════════════════════════════════════════════════════════════════════════════════════
    [BLOCKCHAIN] reportAccountAdded(0x99590c4d…1749f) -> 0x<txHash>
    ```

2. **On-chain** — con el `txHash` confirma que se minó:

    ```bash
    curl -s -X POST -H "Content-Type: application/json" \
      --data '{"jsonrpc":"2.0","method":"eth_getTransactionReceipt","params":["0x<txHash>"],"id":1}' \
      http://127.0.0.1:8545/ | jq '.result | {status, blockNumber, logs}'
    ```

    `status: "0x1"` → registrado en el contrato. `"0x0"` → la tx revertió (la función `require`/`revert` del contrato no se cumplió).

3. **Búsqueda inversa** — encuentra todas las tx del contrato que afectan a una address concreta (sin depender de guardar `txHash`):

    ```bash
    ADDR_TOPIC=0x00000000000000000000000099590c4d8971275e6d90467724d9169ab7d1749f
    curl -s -X POST -H "Content-Type: application/json" --data "{
      \"jsonrpc\":\"2.0\",\"method\":\"eth_getLogs\",\"id\":1,
      \"params\":[{\"fromBlock\":\"earliest\",\"toBlock\":\"latest\",
                   \"address\":\"<CONTRACT>\",\"topics\":[null,\"$ADDR_TOPIC\"]}]
    }" http://127.0.0.1:8545/ | jq '.result | length'
    ```

## Estructura del proyecto

```
besu-permission-rpc-plugin/
├── build.gradle                # toolchain Java 21, fatJar, deps
├── settings.gradle
├── plugin.properties.example   # plantilla de config
├── besu.toml.example           # ejemplo de arranque de Besu
├── install.sh                  # helper de instalación
├── QUICK_START.md
├── CLAUDE.md                   # notas para asistentes de código
└── src/main/
    ├── java/com/example/besu/plugin/
    │   ├── PermissionInterceptorPlugin.java   # entry point (register/start/stop)
    │   ├── PermissionEventCapture.java        # store + log del plugin
    │   ├── PermissionEvent.java               # modelo (EventType enum)
    │   ├── PermissionEventListener.java
    │   ├── NodeInfoProvider.java              # cache del enode local
    │   ├── PermissionRpcInterceptor.java      # wrapper legacy (no usado en runtime)
    │   ├── blockchain/
    │   │   ├── BlockchainReporter.java        # cola de tx + worker
    │   │   ├── ContractTxSender.java          # firma + eth_sendRawTransaction
    │   │   └── NodeKeyLoader.java             # carga la key del nodo (hex 32 bytes)
    │   ├── config/PluginConfig.java
    │   ├── export/EventExporter.java          # util JSON/XML/TSV/HTML (no wired)
    │   └── example/PluginUsageExample.java    # demo, no se empaqueta con Besu
    └── resources/META-INF/services/
        └── org.hyperledger.besu.plugin.BesuPlugin
```

## Tipos de evento

```java
PermissionEvent.EventType.ADD_ACCOUNTS      // perm_addAccountsToAllowlist
PermissionEvent.EventType.REMOVE_ACCOUNTS   // perm_removeAccountsFromAllowlist
PermissionEvent.EventType.ADD_NODES         // perm_addNodesToAllowlist
PermissionEvent.EventType.REMOVE_NODES      // perm_removeNodesFromAllowlist
```

El reporter mapea cada uno a la función correspondiente del contrato:

| EventType | Llamada on-chain |
|---|---|
| `ADD_ACCOUNTS` | `reportAccountAdded(address)` |
| `REMOVE_ACCOUNTS` | `reportAccountRemoved(address)` |
| `ADD_NODES` | `reportEnodeAdded(string)` |
| `REMOVE_NODES` | `reportEnodeRemoved(string)` |

Un evento con `n` items se convierte en `n` transacciones (una por address/enode). Las tx se procesan secuencialmente en un hilo dedicado (`blockchain-reporter`) consumiendo de una `BlockingQueue`.

## Troubleshooting

### `[BLOCKCHAIN ERROR] ... -32007 "Sender account not authorized"`

La cuenta del plugin (la derivada de `blockchain.node.key.path`) no está en el `accounts-allowlist`. Busca su address así:

```bash
grep "Reporter started" permission-events.log
# [BLOCKCHAIN] Reporter started. Sender address: 0x<SENDER>
```

Añádela al allowlist con `perm_addAccountsToAllowlist` y reenvía la alta fallida.

> `BlockchainReporter` **no reintenta** tx fallidas automáticamente. Un evento que fracasó solo se vuelve a reportar si se genera de nuevo (p. ej. repitiendo el `perm_add*`).

### `rpc.port no configurado`

El plugin no pudo resolver el puerto RPC ni desde `plugin.properties` ni desde la `BesuConfiguration` del contexto. Añade `rpc.port=8545` (o el que uses) al fichero de config.

### El enode sale como `unknown`

El resolver llama a `net_enode` en bucle con 3 s de espera, hasta 15 intentos. Si tras ~45 s no lo ha resuelto, revisa que `net` esté en `--rpc-http-api` y que el puerto coincida con `rpc.port`.

### No se escriben logs

```bash
ls -la /var/log/besu/
chmod 755 /var/log/besu
```

Si corres Besu como usuario no privilegiado, apunta `log.file` a una ruta escribible por ese usuario.

## Licencia

MIT.
