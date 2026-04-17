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
4. Wraps existing Besu RPC methods with `PermissionRpcInterceptor` instances
5. Starts optional services: `PermissionRestService`, `MetricsProvider`, `EventNotifier`

### RPC Interception Flow

```
Besu receives JSON-RPC call
  → PermissionRpcInterceptor.execute(request)
    → extracts params (addresses or enodes) from JsonRpcRequestContext
    → calls original RPC method delegate
    → on success: calls PermissionEventCapture.captureAccountPermissionEvent() or captureNodePermissionEvent()
      → stores PermissionEvent in CopyOnWriteArrayList
      → appends to log file
      → notifies PermissionEventListeners
      → optionally triggers EventNotifier webhook
```

### Key Classes

| Class | Role |
|---|---|
| `PermissionInterceptorPlugin` | Entry point; wires all components |
| `PermissionRpcInterceptor` | Wraps a Besu `RpcMethod`; intercepts calls |
| `PermissionEventCapture` | Thread-safe store; logs to file; fires listeners |
| `PermissionEvent` | Immutable event model with `EventType` enum |
| `NodeInfoProvider` | Reads enode/port from `BesuContext` |
| `PluginConfig` | Loads config from file or system properties |
| `PermissionRestService` | Embedded REST API for querying events |
| `MetricsProvider` | Prometheus-compatible metrics endpoint |
| `EventExporter` | Multi-format export (JSON, CSV, XML, TSV, HTML) |
| `EventNotifier` | Webhook delivery (Slack/Discord compatible) |

### Configuration

`PluginConfig` reads from a config file or system properties (prefixed `besu.permission.*`). Key options:

- `log.file` — event log path (default: `/var/log/besu/permission-events.log`)
- `metrics.port` — Prometheus metrics port (default: 9090)
- `api.rest.enabled` — enable embedded REST API (default: true)
- `memory.max.events` — max events kept in memory (default: 10000)
- `notification.webhook` — webhook URL for real-time notifications
- `export.csv.enabled` — enable CSV export (default: true)

Plugin is activated in `besu.toml` via the `plugins` key. See `besu.toml.example` for a complete example.

## Dependencies

- **Besu Plugin API / Ethereum API** 25.8.0 — `compileOnly` (provided by Besu at runtime)
- **Jackson** 2.17.0 — JSON serialization
- **Log4j** 2.23.1 — logging
- **Apache HttpClient 5** 5.3.1 — webhook HTTP calls
- **JUnit 4** 4.13.2 — tests
- Java 17 required

## Important Notes

- Besu API dependencies are `compileOnly` — they must NOT be bundled in the fat JAR (Besu provides them at runtime). The `fatJar` task in `build.gradle` explicitly excludes them.
- `PermissionEventCapture` uses `CopyOnWriteArrayList` for thread safety; reads are lock-free but writes copy the array — avoid tight-loop writes.
- `PluginUsageExample.java` in the `example` package is demonstration code only, not production logic.
