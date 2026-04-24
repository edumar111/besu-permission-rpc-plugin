# Informe de integración para el plugin de Besu — PermissionAuditLog

Documento de handoff para el desarrollador del plugin de Besu que reportará on-chain al contrato `PermissionAuditLog` cada vez que se ejecuten los RPCs de local permissioning (`perm_addAccountsToAllowlist` / `perm_removeAccountsFromAllowlist` / `perm_addNodesToAllowlist` / `perm_removeNodesFromAllowlist`).

El sistema consta de 3 proyectos:

1. **`permissioned-contract-cbweb3`** — Contratos Solidity (Foundry).
2. **`permission-service-agent-cbweb3`** — Agente Node.js/Express que actúa como proxy JSON-RPC y workers RabbitMQ.
3. **`permission-daaps-cbweb3`** — Frontend React/Vite (wagmi + MetaMask).

---

## Direcciones desplegadas (chain 650540, `gasPrice = 0`)

| Contrato | Dirección |
|---|---|
| EntityManager | `0x6c589893132b96B9103F1D673A15eA0AA795BefE` |
| PermissionRules | `0xDcd4901D8B6ff2b48621D4C10749A2B1E9F2bf99` |
| NodePermissionRules | `0x3c52BbC1f12bed9917770Fda1A65cA77500092ae` |
| **PermissionAuditLog** | `0xd69Edd65B0E2605D8E5aE485DAf1a12033B50DC1` |

Fuente: `permission-service-agent-cbweb3/.env` + `permissioned-contract-cbweb3/deploy-*.txt`.

---

## Parte 1 — Proyecto de contratos (`permissioned-contract-cbweb3`)

Foundry. Archivos bajo `src/`:

| Archivo | Propósito |
|---|---|
| `EntityManager.sol` | Registro de entidades y admins globales. Fuente única de verdad del ciclo de vida de entidades y asignación de admins. |
| `PermissionRules.sol` | Lógica de permisos de cuentas. Admins otorgan/revocan cuentas dentro de su entidad; el owner puede overridear. |
| `NodePermissionRules.sol` | Registro de nodos Besu indexado por enode. Trackea rol (Validator/Boot/Writer/Observer), `active`, y entidad. |
| `PermissionAuditLog.sol` | **Contrato que llamará el plugin.** Recibe reportes de nodos Besu autorizados cuando se aplican cuentas/enodes a los allowlists locales. Emite eventos inmutables. |
| `IEntityManager.sol` | Interface que usan PermissionRules y NodePermissionRules para consultar estado de entidad sin acoplamiento directo. |

### EntityManager — funciones clave

| Función | Autorización | Descripción |
|---|---|---|
| `createEntity(string name) → uint256` | owner | Crea una entidad. |
| `setEntityActive(uint256 entityId, bool active)` | owner | Activa/desactiva entidad. |
| `grantPermissionAdmin(address account, uint256 entityId)` | owner | Asigna admin a entidad. |
| `revokePermissionAdmin(address account)` | owner | Revoca admin. |
| `isAdmin(address)` → `bool` | view | Verifica rol admin. |
| `getAdminEntity(address)` → `uint256` | view | Entidad del admin (0 = sistema). |
| `getEntity(uint256)` → `(name, active)` | view | Metadata de entidad. |
| `isEntityActive(uint256)` → `bool` | view | Estado activo. |

**Eventos:** `EntityCreated`, `EntityStatusChanged`, `AdminAssignedToEntity`, `AdminRemovedFromEntity`, `AdminGranted`, `AdminRevoked`.

### PermissionRules — funciones clave

| Función | Autorización |
|---|---|
| `grantPermission(address account)` | admin de la entidad |
| `revokePermission(address account)` | admin de la entidad |
| `ownerGrantPermission(address account, uint256 entityId)` | owner |
| `ownerRevokePermission(address account)` | owner |
| `hasPermission(address)` → `bool` | view |
| `getAccountEntity(address)` → `uint256` | view |
| `getEntityAccounts(uint256)` → `address[]` | view |

**Eventos:** `PermissionGranted`, `PermissionRevoked`, `AccountAddedToEntity`, `AccountRemovedFromEntity`.

### NodePermissionRules — funciones clave

NodeRole enum: `Unknown=0, Validator=1, Boot=2, Writer=3, Observer=4`.

| Función | Autorización |
|---|---|
| `ownerAddNode(string enode, uint8 role, bool active, uint16 rpcPort, address nodeAddress, uint256 entityId)` | owner |
| `ownerRemoveNode(string enode)` | owner |
| `updateNodeRole(string enode, uint8 newRole)` | owner |
| `ownerSetNodeActive(string enode, bool active)` | owner |
| `setNodeActive(string enode, bool active)` | admin (dentro de su entidad) |
| `getNode(string)` → `(enode, role, active, rpcPort, nodeAddress)` | view |
| `getNodeEntity(string)` → `uint256` | view |
| `getEnodeIdByAddress(address nodeAddr)` → `bytes32` | view (**relevante para el plugin**) |
| `getEntityNodeIds(uint256)` → `bytes32[]` | view |
| `isNodeAllowed(string)` → `bool` | view |
| `getAllValidators()` → `string[]` | view |
| `getAllBootnodes()` → `string[]` | view |

**Eventos:** `NodeAdded`, `NodeRemoved`, `NodeRoleChanged`, `NodeStatusChanged`, `NodeAddedToEntity`, `NodeRemovedFromEntity`.

Nota crítica: `enodeId = keccak256(bytes(enode))`. Todas las APIs internas lo usan como bytes32; la string completa solo la conocen `NodeAdded` (evento) y `getNode(enode)`.

### PermissionAuditLog — contrato objetivo del plugin

**Autorización:** Solo Ethereum addresses registradas como nodos en `NodePermissionRules` pueden llamar las funciones `report*`. El contrato usa `nodeRules.getEnodeIdByAddress(msg.sender)` para autenticar y resolver la identidad del nodo reportante. Si `msg.sender` no está registrado, revierte.

**Funciones write (todas `onlyRegisteredNode`):**

```solidity
function reportAccountAdded(address account) external;
function reportAccountRemoved(address account) external;
function reportEnodeAdded(string calldata enode) external;
function reportEnodeRemoved(string calldata enode) external;
```

Cada una:
- Resuelve `nodeEnodeId = getEnodeIdByAddress(msg.sender)` — identidad del nodo reportante.
- Actualiza `ApplyRecord { bool applied, uint64 addedAt, uint64 removedAt }` en mapping `account → nodeEnodeId → ApplyRecord` (o `enodeId → ...` para enodes).
- Emite evento.

**Funciones read (view):**

```solidity
function getAccountAppliedNodes(address account) external view returns (bytes32[] memory);
function getAccountRecord(address account, bytes32 nodeEnodeId) external view returns (ApplyRecord memory);
function getEnodeAppliedNodes(string calldata enode) external view returns (bytes32[] memory);
function getEnodeRecord(string calldata enode, bytes32 nodeEnodeId) external view returns (ApplyRecord memory);
```

**Eventos emitidos:**

```solidity
event AccountApplied(
    address indexed account,
    bytes32 indexed nodeEnodeId,
    bool added,
    uint64 timestamp
);

event EnodeApplied(
    bytes32 indexed enodeId,
    bytes32 indexed nodeEnodeId,
    bool added,
    uint64 timestamp
);
```

- `added = true` si fue un report de add; `false` si remove.
- `timestamp` = `block.timestamp`.

---

## Parte 2 — Agente backend (`permission-service-agent-cbweb3`)

Proxy JSON-RPC en Express.js + 3 workers RabbitMQ.

### Arquitectura

```
Frontend (React/wagmi) 
    ↓  (firma local, envía raw signed tx)
Express /rpc   (src/server.js)
    ↓
Router        (src/routes/nodeRoutes.js) — decodifica tx por selector+dirección
    ├─ Broadcast a Besu
    └─ Publica a cola RabbitMQ
         ↓
Workers:
  ├─ addNodeWorker    — consume colas → llama perm_*ToAllowlist en Besu
  ├─ eventIndexerWorker — poll de eventos on-chain → publica a exchange "audit"
  └─ auditWorker      — consume exchange "audit" → escribe logs/audit.jsonl
```

### Entry point: `src/server.js`

- Puerto **3000** (configurable en `.env`).
- Endpoints:
    - `POST /rpc` — JSON-RPC principal.
    - `POST /audit-rpc` — dedicado a `PermissionAuditLog` (`src/routes/auditRoutes.js`).
    - `GET /audit?page=1&pageSize=50&event=...` — paginado desde `logs/audit.jsonl`.
    - `GET /entities/:id/admins` — admins on-chain.
    - `GET /entities/:id/accounts` — accounts on-chain.
    - `GET /entities/:id/nodes` — nodos enriquecidos (id→enode via audit log).
- Arranca 3 workers + conexión RabbitMQ en paralelo.

### Router: `src/routes/nodeRoutes.js`

Decodifica por `tx.to` (dirección del contrato) + selector (4 bytes de `tx.data`). Dispatch:

| Contrato destino | Método | Handler |
|---|---|---|
| NodePermissionRules | `ownerAddNode` | `handleOwnerAddNodeInternal` |
| NodePermissionRules | `ownerRemoveNode` | `handleOwnerRemoveNodeInternal` |
| NodePermissionRules | `updateNodeRole` | `handleUpdateNodeRoleInternal` |
| NodePermissionRules | `ownerSetNodeActive` | `handleOwnerSetNodeActiveInternal` |
| NodePermissionRules | `setNodeActive` | `handleSetNodeActiveInternal` |
| EntityManager | `createEntity` | `handleCreateEntityInternal` |
| EntityManager | `setEntityActive` | `handleSetEntityActiveInternal` |
| EntityManager | `grantPermissionAdmin` | `handleGrantPermissionAdminInternal` |
| EntityManager | `revokePermissionAdmin` | `handleRevokePermissionAdminInternal` |
| PermissionRules | `grantPermission` | `handleGrantPermissionInternal` |
| PermissionRules | `revokePermission` | `handleRevokePermissionInternal` |
| PermissionRules | `ownerGrantPermission` | `handleOwnerGrantPermissionInternal` |
| PermissionRules | `ownerRevokePermission` | `handleOwnerRevokePermissionInternal` |
| PermissionAuditLog | `reportAccountAdded` | `handleReportAccountAddedInternal` |
| PermissionAuditLog | `reportAccountRemoved` | `handleReportAccountRemovedInternal` |
| PermissionAuditLog | `reportEnodeAdded` | `handleReportEnodeAddedInternal` |
| PermissionAuditLog | `reportEnodeRemoved` | `handleReportEnodeRemovedInternal` |

Cada handler:
1. Decodifica la raw signed tx.
2. Captura contexto relevante (entityId del signer vía `getAdminEntity`, oldRole, etc.) **antes** del broadcast (importante para revokes que borran el mapping post-tx).
3. Hace broadcast a Besu (`sendSignedTx*`) y espera confirmación en bloque.
4. **Publica mensaje a cola RabbitMQ correspondiente** con payload.
5. Devuelve `tx.hash` al cliente.

### Colas RabbitMQ (todas durables)

| Cola | Payload |
|---|---|
| `ownerAddNode` | `{enode, role, active, rpcPort, nodeAddress, entityId, txHash}` |
| `ownerRemoveNode` | `{enode, role, txHash}` |
| `updateNodeRole` | `{enode, oldRole, newRole, txHash}` |
| `ownerSetNodeActive` | `{enode, role, active, txHash}` |
| `setNodeActive` | `{enode, role, active, txHash}` |
| `createEntity` | `{name, txHash}` |
| `setEntityActive` | `{entityId, active, txHash}` |
| `grantPermissionAdmin` | `{account, entityId, txHash}` |
| `revokePermissionAdmin` | `{account, entityId, txHash}` |
| `grantPermission` | `{account, entityId, txHash}` |
| `revokePermission` | `{account, entityId, txHash}` |
| `ownerGrantPermission` | `{account, entityId, txHash}` |
| `ownerRevokePermission` | `{account, entityId, txHash}` |

### Exchange de auditoría

- **Nombre:** `audit`, type `topic`, durable.
- **Publisher:** `eventIndexerWorker`.
- **Consumer:** `auditWorker`.
- Routing keys: `entity.*`, `permission.*`, `node.*`, **`auditlog.*`** (incluye `auditlog.AccountApplied` y `auditlog.EnodeApplied`).

### Worker: `src/workers/addNodeWorker.js`

Consume las 13 colas. Para cada mensaje:

1. Determina los **nodos destino** según el tipo:
    - **Nodos (add/remove/update)** — usa `propagationScope(role)`:
        - `Validator`/`Boot` → todos los bootnodes + validators.
        - `Writer` → solo bootnodes.
        - `Observer`/`Unknown` → ninguno.
    - **Cuentas/Admins (grant/revoke)** — `getAccountTargetNodes(entityId)`:
        - Base: todos los bootnodes + validators (scope global).
        - Más: los **Writer nodes activos de la entidad** de la cuenta/admin (desde `data/node-index.json`).
        - Fallback on-chain si el índice off-chain está vacío (resuelve `bytes32 id → enode` via `audit.jsonl`).
2. Para cada nodo destino:
    - Extrae `ip` del enode (formato `enode://pubkey@ip:port`).
    - Consulta `rpcPort` on-chain via `getNode(enode)`.
    - Llama `perm_add/removeAccountsToAllowlist` o `perm_add/removeNodesToAllowlist` vía HTTP JSON-RPC contra `ip:rpcPort`. **Aquí es donde el plugin observa.**

### Cliente RPC: `src/rpc/besuRpcClient.js` — **donde el plugin se integra**

Funciones exportadas:

| Función | RPC Besu que dispara |
|---|---|
| `addNodeToBootnodeAllowlist(enode, ip, rpcPort)` | `perm_addNodesToAllowlist` |
| `removeNodeFromBootnodeAllowlist(enode, ip, rpcPort)` | `perm_removeNodesFromAllowlist` |
| `addAccountToAllowlist(account, ip, rpcPort)` | **`perm_addAccountsToAllowlist`** |
| `removeAccountFromAllowlist(account, ip, rpcPort)` | **`perm_removeAccountsFromAllowlist`** |

Forma del request POST:

```json
{
  "jsonrpc": "2.0",
  "method": "perm_addAccountsToAllowlist",
  "params": [["0x1234..."]],
  "id": 1
}
```

### Worker: `src/workers/eventIndexerWorker.js`

Polling cada `POLL_INTERVAL_SECONDS` (default 10s) con `queryFilter("*", from, to)` sobre los 4 contratos, paginado en chunks de `BLOCK_CHUNK_SIZE` (default 500).

Responsabilidades:

1. **Índice off-chain de cuentas** — `data/entity-index.json`: `entityId → [accounts]` desde `AccountAddedToEntity` / `AccountRemovedFromEntity`.
2. **Índice off-chain de nodos** — `data/node-index.json`: `bytes32 id → {enode, role, active, rpcPort, nodeAddress, entityId}` desde eventos de NodePermissionRules. Rehidratación one-shot desde genesis cuando el archivo falta.
3. **Propagación de `setEntityActive`** — al detectar `EntityStatusChanged`, itera accounts de la entidad y llama `perm_add/remove` en todos los nodos (este flujo NO pasa por el addNodeWorker).
4. **Audit trail** — publica **todos** los eventos al exchange `audit` con routing keys `entity.*`, `permission.*`, `node.*`, **`auditlog.*`**.
5. **Checkpoint** — `data/entity-checkpoint.json` (`{lastBlock: N}`) — resume polling desde el último bloque procesado tras reinicio.

### Worker: `src/workers/auditWorker.js`

- Bind al exchange `audit` con routing `#` (captura todo).
- Escribe cada evento a `logs/audit.jsonl` — una línea JSON por evento.
- Formato: `{timestamp, contract, event, blockNumber, txHash, logIndex, args}`.

### Archivos de datos persistentes

| Archivo | Contenido |
|---|---|
| `data/entity-index.json` | `{ "1": ["0x...", "0x..."] }` — accounts por entidad. |
| `data/node-index.json` | `{ "<bytes32-id>": {enode, role, active, rpcPort, nodeAddress, entityId} }` — nodos indexados por id. |
| `data/entity-checkpoint.json` | `{"lastBlock": N}` — último bloque procesado por el indexer. |
| `logs/audit.jsonl` | Log append-only de todos los eventos. Una línea JSON por evento. |

### Variables de entorno (`.env`)

| Variable | Descripción |
|---|---|
| `RPC_URL` | Endpoint JSON-RPC del nodo Besu que usa el agente. |
| `ENTITY_MANAGER_ADDRESS` | Dirección del contrato. |
| `PERMISSION_RULES_ADDRESS` | Dirección del contrato. |
| `NODE_PERMISSION_RULES_ADDRESS` | Dirección del contrato (acepta CSV de direcciones legacy). |
| `PERMISSION_AUDIT_LOG_ADDRESS` | Dirección del contrato. |
| `RABBITMQ_URL` / `RABBIT_URL` | Broker (`amqp://user:pass@host:5672/`). |
| `PORT` | Puerto HTTP (default 3000). |
| `POLL_INTERVAL_SECONDS` | Polling del indexer (default 10). |
| `BLOCK_CHUNK_SIZE` | Chunk size para `eth_getLogs` (default 500). |

---

## Parte 3 — Frontend dApp (`permission-daaps-cbweb3`)

Stack: React 18 + TypeScript + Vite + wagmi/viem + Tailwind + sonner (toasts) + Motion.

### Entry: `src/App.tsx`

Rutas:
- `/` — home pública.
- `/dashboard` — overview autenticado.
- `/entities` — gestión de entidades (owner).
- `/entities/:id/admins` — admins de una entidad (owner).
- `/entities/:id/nodes` — nodos de una entidad (owner).
- `/admin/accounts` — grant/revoke cuentas (admin).
- `/audit` — audit log paginado.

### Hooks principales (`src/hooks/`)

| Hook | Propósito |
|---|---|
| `usePermissionRules` | Read+write para EntityManager + PermissionRules. Wrappers wagmi de `useReadContract` / `useWriteContract`. |
| `useNodePermissionRules` | Read+write para NodePermissionRules. |
| `useAudit` | Lee `GET /audit` y `GET /entities/:id/admins|accounts|nodes` del agente. Hooks optimistas + reconcile con polling tras mutación. |
| `useEntities` | Conveniencia sobre entidades. |
| `useRole` | Resuelve rol actual del usuario (owner / admin / user) desde on-chain. |

### Configuración

- `src/config/env.ts` — carga env vars.
- `src/contracts/addresses.ts` — direcciones por chainId.
- `src/contracts/*Abi.ts` — ABIs (uno por contrato).
- wagmi config apunta a `http://<agente>:3000/rpc` como transport — **todo el tráfico de escritura pasa por el agente**, no directamente a Besu.

### Flujo de escritura típico

1. Usuario hace click en "Otorgar permiso".
2. Hook invoca `writeContractAsync({address, abi, functionName, args})` de wagmi.
3. MetaMask firma localmente; wagmi envía `eth_sendRawTransaction` al RPC configurado (= agente).
4. wagmi espera receipt, muestra toast (loading → success/error).
5. Hook reconcile invalida caché y refresca.

---

## Parte 4 — Circuito completo (el punchline para el plugin dev)

Qué pasa end-to-end cuando un admin hace click en **"Otorgar permiso"** a una cuenta:

```
1. FRONTEND
   useGrantPermission(0x1234).write()
     → wagmi encode grantPermission(0x1234)
     → MetaMask firma
     → POST eth_sendRawTransaction(rawSigned) → http://agent:3000/rpc

2. AGENTE (nodeRoutes.js → handleGrantPermissionInternal)
   a) decode tx → detecta selector de grantPermission
   b) signer = tx.from (admin)
   c) entityId = await getAdminEntity(signer)       ← captura ANTES del broadcast
   d) await sendSignedTxPermission(rawSigned)       ← broadcast a Besu
   e) await tx.wait()                                ← confirma en bloque
   f) publish("grantPermission", {account, entityId, txHash})  ← RabbitMQ
   g) responde {jsonrpc, result: tx.hash, id}

3. BESU (on-chain)
   PermissionRules.grantPermission(0x1234)
     → emite PermissionGranted(account, by)
     → emite AccountAddedToEntity(entityId, account, by)

4. AGENTE (addNodeWorker consume "grantPermission")
   a) targets = bootnodes + validators + writers(entityId=1)
   b) para cada target:
        - ip = extractIp(enode)
        - rpcPort = (await getNode(enode)).rpcPort
        - POST http://ip:rpcPort con {method: "perm_addAccountsToAllowlist", params: [[account]]}
          ╔═══════════════════════════════════════════════════════════════╗
          ║  ← AQUÍ ENTRA EL PLUGIN                                        ║
          ║  El plugin observa perm_addAccountsToAllowlist.                ║
          ║  Tras aplicar la cuenta al allowlist local, el plugin firma   ║
          ║  y envía:                                                      ║
          ║    PermissionAuditLog.reportAccountAdded(0x1234)              ║
          ║  con la key del propio nodo Besu (nodeAddress registrado en   ║
          ║  NodePermissionRules).                                         ║
          ╚═══════════════════════════════════════════════════════════════╝

5. BESU (on-chain)
   PermissionAuditLog.reportAccountAdded(0x1234)
     → valida: msg.sender está registrado (getEnodeIdByAddress != 0)
     → escribe ApplyRecord {applied: true, addedAt: block.timestamp}
     → emite AccountApplied(account=0x1234, nodeEnodeId, added=true, timestamp)

6. AGENTE (eventIndexerWorker, polling)
   detecta AccountApplied
     → publishToExchange("audit", "auditlog.AccountApplied", record)

7. AGENTE (auditWorker consume exchange "audit")
   append a logs/audit.jsonl

8. FRONTEND
   useAudit() hace polling a GET /audit → ve el nuevo AccountApplied
   useReconcileEntityAccounts() refresca la lista
   Usuario ve la cuenta "aplicada" en todos los nodos objetivo
```

Los mismos 8 pasos aplican, mutatis mutandis, para:
- `revokePermission` → `perm_removeAccountsFromAllowlist` → `reportAccountRemoved` → `AccountApplied(added=false)`.
- `ownerAddNode` (rol Validator/Boot/Writer) → `perm_addNodesToAllowlist` → `reportEnodeAdded` → `EnodeApplied(added=true)`.
- `ownerRemoveNode` → `perm_removeNodesFromAllowlist` → `reportEnodeRemoved` → `EnodeApplied(added=false)`.

---

## Parte 5 — Qué tiene que hacer el plugin exactamente

### Triggers que debe observar

Los 4 RPCs de local permissioning de Besu:

| RPC | Cuándo se invoca | Reporte a hacer |
|---|---|---|
| `perm_addAccountsToAllowlist([addr, ...])` | Agente propaga grant de cuenta/admin | `reportAccountAdded(addr)` por cada dirección |
| `perm_removeAccountsFromAllowlist([addr, ...])` | Agente propaga revoke de cuenta/admin | `reportAccountRemoved(addr)` por cada dirección |
| `perm_addNodesToAllowlist([enode, ...])` | Agente propaga `ownerAddNode` / activación | `reportEnodeAdded(enode)` por cada enode |
| `perm_removeNodesFromAllowlist([enode, ...])` | Agente propaga `ownerRemoveNode` / desactivación | `reportEnodeRemoved(enode)` por cada enode |

### Contrato objetivo y firma

**Dirección:** `0xd69Edd65B0E2605D8E5aE485DAf1a12033B50DC1`
**Chain ID:** `650540`
**Gas price:** `0`

Firmas:

```solidity
function reportAccountAdded(address account) external;
function reportAccountRemoved(address account) external;
function reportEnodeAdded(string calldata enode) external;
function reportEnodeRemoved(string calldata enode) external;
```

### Clave de firma

La tx la firma **el propio nodo Besu** con la key cuya Ethereum address está registrada en `NodePermissionRules` (campo `nodeAddress` pasado a `ownerAddNode`). La clave típicamente es la misma del `--node-private-key-file` de Besu (derivable del enode pubkey).

El contrato autentica `msg.sender` llamando internamente a `nodeRules.getEnodeIdByAddress(msg.sender)` — si devuelve `bytes32(0)`, revierte. Por eso el plugin NO puede firmar con una key arbitraria.

### Pseudocódigo del plugin

```java
// Simplificado (Besu plugins son Java)
public class PermissionAuditLogReporter implements BesuPlugin {

    private Web3j web3;
    private Credentials nodeCredentials;     // key del propio nodo
    private String auditLogAddress = "0xd69Edd65B0E2605D8E5aE485DAf1a12033B50DC1";

    @Override
    public void onRpcCall(String method, Object[] params) {
        switch (method) {
            case "perm_addAccountsToAllowlist": {
                List<String> accounts = (List<String>) params[0];
                for (String acc : accounts) {
                    sendReport("reportAccountAdded(address)", new Object[]{acc});
                }
                break;
            }
            case "perm_removeAccountsFromAllowlist": {
                List<String> accounts = (List<String>) params[0];
                for (String acc : accounts) {
                    sendReport("reportAccountRemoved(address)", new Object[]{acc});
                }
                break;
            }
            case "perm_addNodesToAllowlist": {
                List<String> enodes = (List<String>) params[0];
                for (String en : enodes) {
                    sendReport("reportEnodeAdded(string)", new Object[]{en});
                }
                break;
            }
            case "perm_removeNodesFromAllowlist": {
                List<String> enodes = (List<String>) params[0];
                for (String en : enodes) {
                    sendReport("reportEnodeRemoved(string)", new Object[]{en});
                }
                break;
            }
        }
    }

    private void sendReport(String signature, Object[] args) {
        // encode ABI
        String data = AbiEncoder.encode(signature, args);
        // build, sign, send con gasPrice=0
        RawTransaction tx = RawTransaction.createTransaction(
            nonce, BigInteger.ZERO /* gasPrice */, BigInteger.valueOf(300_000),
            auditLogAddress, BigInteger.ZERO, data
        );
        byte[] signed = TransactionEncoder.signMessage(tx, 650540L, nodeCredentials);
        web3.ethSendRawTransaction(Numeric.toHexString(signed)).send();
    }
}
```

### Consideraciones

1. **Idempotencia** — los RPCs `perm_*` de Besu son idempotentes (agregar algo que ya está no falla). El plugin debe ser tolerante a reportes duplicados: el contrato sobrescribe el timestamp pero mantiene consistencia.
2. **Fallos transitorios** — si la tx de report falla (nonce collision, RPC caído), el plugin debería reintentar con backoff. El registro on-chain es best-effort; la consistencia verdadera la tiene el allowlist local de Besu.
3. **Orden de operaciones** — aplicar PRIMERO el allowlist local, LUEGO reportar. Si el report falla, el allowlist igual queda aplicado (estado local es la fuente de verdad).
4. **Batching** — si `perm_addAccountsToAllowlist` recibe un array de N direcciones, conviene encolarlas y enviar N reports sin bloquear la respuesta del RPC.
5. **Nonce management** — usar nonce del nodo a través de `eth_getTransactionCount` con caché local; una cola serializa las txs para evitar colisiones.
6. **GasPrice = 0** — no es necesario fondear la account del nodo, pero sí que exista y esté en el allowlist de cuentas (lo cual se cumple porque `ownerAddNode` registra `nodeAddress` y éste tiene permiso implícito como nodo).

### Eventos que el plugin emite (indirectamente, via el contrato)

```solidity
event AccountApplied(
    address indexed account,
    bytes32 indexed nodeEnodeId,
    bool added,
    uint64 timestamp
);

event EnodeApplied(
    bytes32 indexed enodeId,
    bytes32 indexed nodeEnodeId,
    bool added,
    uint64 timestamp
);
```

Estos eventos los captura `eventIndexerWorker`, pasan al exchange `audit` con routing `auditlog.AccountApplied` / `auditlog.EnodeApplied`, `auditWorker` los escribe a `logs/audit.jsonl`, y el frontend los muestra en `/audit`.

---

## Diagrama resumen

```
┌──────────────────────────────────────────────────────────┐
│  FRONTEND (React/wagmi) — usuario firma con MetaMask     │
└───────────────────────────┬──────────────────────────────┘
                            │ POST /rpc eth_sendRawTransaction
                            ▼
┌──────────────────────────────────────────────────────────┐
│  AGENTE — nodeRoutes.js                                  │
│  • decode tx → selector + dirección                      │
│  • broadcast a Besu                                      │
│  • publish RabbitMQ                                      │
└─────┬──────────────────────────────────────────┬─────────┘
      │                                          │
      ▼                                          ▼
┌──────────────────┐                   ┌──────────────────┐
│  BESU blockchain │                   │  RabbitMQ queues │
│  PermissionRules │                   │  grantPermission │
│  .grantPermission│                   │  ...             │
│  → emite eventos │                   └─────────┬────────┘
└────────┬─────────┘                             │
         │                                       ▼
         │                           ┌──────────────────────┐
         │                           │  addNodeWorker       │
         │                           │  perm_add/removeAcc  │
         │                           │  perm_add/removeNod  │
         │                           └──────────┬───────────┘
         │                                      │
         │                                      ▼
         │                ┌─────────────────────────────────────┐
         │                │  BESU LOCAL PERMISSIONING RPCs      │
         │                │   perm_addAccountsToAllowlist       │
         │                │   perm_removeAccountsFromAllowlist  │
         │                │   perm_addNodesToAllowlist          │
         │                │   perm_removeNodesFromAllowlist     │
         │                ├─────────────────────────────────────┤
         │                │   ⇐ PLUGIN OBSERVA AQUÍ ⇒           │
         │                │   Llama PermissionAuditLog.report*  │
         │                └─────────────────┬───────────────────┘
         │                                  │
         ▼                                  ▼
┌──────────────────────────────────────────────────────────┐
│  PermissionAuditLog on-chain                             │
│  → ApplyRecord storage                                   │
│  → AccountApplied / EnodeApplied events                  │
└───────────────────────────┬──────────────────────────────┘
                            │ polled
                            ▼
┌──────────────────────────────────────────────────────────┐
│  eventIndexerWorker → exchange "audit"                   │
│  auditWorker → logs/audit.jsonl                          │
│  GET /audit → frontend                                   │
└──────────────────────────────────────────────────────────┘
```

---

## Referencias rápidas de archivos

### permissioned-contract-cbweb3
- `src/PermissionAuditLog.sol` — el contrato objetivo.
- `src/NodePermissionRules.sol` — `getEnodeIdByAddress(address)` — cómo el AuditLog autentica.
- `deploy-audit.txt`, `deploy-v2.txt`, `deploy-node-v2.txt` — direcciones desplegadas.

### permission-service-agent-cbweb3
- `src/server.js` — entry HTTP + workers.
- `src/routes/nodeRoutes.js` — dispatch por selector.
- `src/routes/auditRoutes.js` — endpoint específico para PermissionAuditLog.
- `src/workers/addNodeWorker.js` — consume colas, llama `perm_*`.
- `src/workers/eventIndexerWorker.js` — poll on-chain, exchange `audit`.
- `src/workers/auditWorker.js` — escribe `logs/audit.jsonl`.
- `src/workers/nodeIndex.js` — índice off-chain de nodos por entidad.
- `src/rpc/besuRpcClient.js` — **donde nacen las llamadas `perm_*ToAllowlist` que el plugin observa**.
- `src/contracts/nodePermission.js`, `permissionRules.js`, `entityManager.js`, `auditLog.js` — wrappers ethers.

### permission-daaps-cbweb3
- `src/App.tsx` — rutas.
- `src/pages/` — Entities, EntityAdmins, EntityNodes, AdminAccounts, Audit.
- `src/hooks/` — usePermissionRules, useNodePermissionRules, useAudit, useRole.
- `src/contracts/addresses.ts` — direcciones on-chain.
- `src/contracts/*Abi.ts` — ABIs.
