# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Full build including tests
./gradlew build -x test      # Build without tests
./gradlew jar                # Compile standard JAR only
./gradlew fatJar             # Build fat JAR with all dependencies (used for deployment)
./gradlew clean              # Clean build artifacts
./gradlew test               # Run tests only
```

The deployable artifact is the fat JAR produced by `./gradlew fatJar`, which must be placed in Besu's plugins directory (e.g., `/opt/besu/plugins/`).

## Architecture Overview

This is a **Hyperledger Besu plugin** (version 2.0.0) that intercepts JSON-RPC permission calls (`perm_addAccountsToAllowlist`, `perm_addNodesToAllowlist`) to capture audit events with node identity information.

### Plugin Lifecycle

Besu discovers the plugin via the Java Service Provider Interface: `src/main/resources/META-INF/services/org.hyperledger.besu.plugin.BesuPlugin` points to `PermissionInterceptorPlugin`. Besu calls `register()` → `start()` → `stop()` on it.

**`PermissionInterceptorPlugin`** is the root — it wires everything together:
1. Obtains the `BesuContext` from Besu on `register()`
2. Creates a `NodeInfoProvider` (extracts enode, ports from context)
3. Creates a `PermissionEventCapture` (thread-safe event store + file logger)
4. On `start()`: resolves the local enode via `net_enode` RPC, starts a file watcher on `permissions_config.toml` to detect ADD/REMOVE of accounts and nodes, and (if enabled) starts the `BlockchainReporter` that reports each change to the `PermissionAuditLog` contract.

### Event Flow

```
Besu writes permissions_config.toml (after perm_addAccountsToAllowlist / perm_addNodesToAllowlist / perm_remove*)
  → file watcher diffs before/after sets
    → PermissionEventCapture.captureAccount*/captureNode* per added/removed item
      → stores PermissionEvent in CopyOnWriteArrayList
      → appends a formatted block to the log file
      → notifies PermissionEventListeners → BlockchainReporter enqueues a signed tx to PermissionAuditLog
```

### Key Classes

| Class | Role |
|---|---|
| `PermissionInterceptorPlugin` | Entry point; wires all components; runs the permissions file watcher |
| `PermissionEventCapture` | Thread-safe event store; appends formatted blocks to the log file; fires listeners |
| `PermissionEvent` | Immutable event model with `EventType` enum (ADD/REMOVE × ACCOUNTS/NODES) |
| `NodeInfoProvider` | Stores the local enode resolved via `net_enode` |
| `PluginConfig` | Loads config from file or system properties |
| `blockchain.BlockchainReporter` | Listener that signs and sends `reportAccountAdded/Removed` / `reportEnodeAdded/Removed` tx to the audit contract |
| `blockchain.ContractTxSender` | Builds, signs, and submits raw EIP-155 transactions via `eth_sendRawTransaction` |
| `blockchain.NodeKeyLoader` | Loads the node private key (hex 32 bytes) and derives credentials |
| `export.EventExporter` | Utility to render events as JSON/XML/TSV/HTML (not wired into runtime) |

### Configuration

`PluginConfig` reads from a properties file (path via `-Dbesu.permission.config=...`) or system properties (prefixed `besu.permission.*`). Key options:

- `log.file` — event log path (default: `/var/log/besu/permission-events.log`)
- `memory.max.events` — max events kept in memory (default: 10000)
- `data.path` / `rpc.port` / `rpc.host` — used to locate `permissions_config.toml` and reach the local RPC
- `blockchain.enabled` — turn on reporting to `PermissionAuditLog`
- `blockchain.contract.address` / `blockchain.chain.id` / `blockchain.node.key.path` / `blockchain.gas.price` / `blockchain.gas.limit` — required when `blockchain.enabled=true`
- `blockchain.rpc.url` — optional override; defaults to `http://127.0.0.1:${rpc.port}/`

Plugin is activated in `besu.toml` via the `plugins` key. See `besu.toml.example` and `plugin.properties.example`.

## Dependencies

- **Besu Plugin API / Ethereum API** 25.8.0 — `compileOnly` (provided by Besu at runtime)
- **Jackson** 2.17.0 — JSON parsing for `net_enode` and RPC calls
- **Log4j** 2.23.1 — logging (`compileOnly`; Besu ships it)
- **Apache HttpClient 5** 5.3.1 — used by `ContractTxSender` and the `net_enode` resolver
- **web3j** 4.12.3 (`crypto`, `abi`, `rlp`, `utils`) — key loading, ABI encoding, tx signing
- **JUnit 5** — tests
- Java 21 required (toolchain declared in `build.gradle`)

## Important Notes

- Besu API dependencies are `compileOnly` — they must NOT be bundled in the fat JAR (Besu provides them at runtime). The `fatJar` task in `build.gradle` explicitly excludes them.
- `PermissionEventCapture` uses `CopyOnWriteArrayList` for thread safety; reads are lock-free but writes copy the array — avoid tight-loop writes.
- `PluginUsageExample.java` in the `example` package is demonstration code only, not production logic.
