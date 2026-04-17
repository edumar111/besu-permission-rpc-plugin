package com.example.besu.plugin;

import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.BesuConfiguration;
import org.hyperledger.besu.plugin.services.RpcEndpointService;
import com.example.besu.plugin.config.PluginConfig;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class PermissionInterceptorPlugin implements BesuPlugin {

    private ServiceManager besuContext;
    private PermissionEventCapture eventCapture;
    private NodeInfoProvider nodeInfoProvider;
    private PluginConfig config;
    private Path dataPath;
    private volatile Thread watcherThread;

    @Override
    public void register(final ServiceManager context) {
        this.besuContext = context;

        try {
            String configPath = System.getProperty("besu.permission.config");
            this.config = configPath != null
                    ? PluginConfig.loadFromFile(configPath)
                    : PluginConfig.loadFromSystemProperties();
        } catch (IOException e) {
            System.err.println("[ERROR] Cargando configuración: " + e.getMessage());
            this.config = new PluginConfig();
        }

        this.eventCapture = new PermissionEventCapture(config.getLogFile());
        this.nodeInfoProvider = new NodeInfoProvider(context);

        // Resolve data path from Besu configuration
        this.dataPath = context.getService(BesuConfiguration.class)
                .map(BesuConfiguration::getDataPath)
                .orElse(Paths.get(System.getProperty("besu.data.path", ".")));

        System.out.println("[BESU PERMISSION RPC PLUGIN] Registrando plugin... data-path=" + dataPath);
    }

    @Override
    public void start() {
        boolean hasRpc = besuContext.getService(RpcEndpointService.class).isPresent();

        eventCapture.logStartup(
                config.getLogFile(),
                String.valueOf(config.getMetricsPort()),
                config.isEnableRestApi(),
                config.isEnableMetrics(),
                config.isEnableCsvExport(),
                config.getMaxEventsInMemory(),
                config.getNotificationWebhook() != null ? config.getNotificationWebhook() : "disabled"
        );

        if (hasRpc) {
            startPermissionsFileWatcher();
            System.out.println("[✓] PermissionInterceptor Plugin iniciado - monitoreando " + dataPath);
        } else {
            System.err.println("[✗] RpcEndpointService no disponible — RPC no habilitado en este nodo");
        }

        config.printConfig();
    }

    private void startPermissionsFileWatcher() {
        Path permFile = dataPath.resolve("permissions_config.toml");

        watcherThread = new Thread(() -> {
            try {
                // Read initial state
                Set<String> knownAccounts = readAllowlist(permFile, "accounts-allowlist");
                Set<String> knownNodes   = readAllowlist(permFile, "nodes-allowlist");

                WatchService watcher = dataPath.getFileSystem().newWatchService();
                dataPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

                System.out.println("[✓] Watcheando permisos en: " + permFile);

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watcher.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                        Path changed = (Path) event.context();
                        if (!changed.getFileName().toString().equals("permissions_config.toml")) continue;

                        Thread.sleep(150); // wait for Besu to finish writing

                        Set<String> newAccounts = readAllowlist(permFile, "accounts-allowlist");
                        Set<String> addedAccounts = diff(knownAccounts, newAccounts);
                        if (!addedAccounts.isEmpty()) {
                            eventCapture.captureAccountPermissionEvent(
                                    nodeInfoProvider.getNodeEnode(), new ArrayList<>(addedAccounts));
                        }
                        knownAccounts = newAccounts;

                        Set<String> newNodes = readAllowlist(permFile, "nodes-allowlist");
                        Set<String> addedNodes = diff(knownNodes, newNodes);
                        if (!addedNodes.isEmpty()) {
                            eventCapture.captureNodePermissionEvent(
                                    nodeInfoProvider.getNodeEnode(), new ArrayList<>(addedNodes));
                        }
                        knownNodes = newNodes;
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[ERROR] Watcher de permisos: " + e.getMessage());
            }
        }, "permission-file-watcher");

        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private Set<String> readAllowlist(Path file, String key) {
        try {
            if (!Files.exists(file)) return new HashSet<>();
            String content = Files.readString(file);
            int idx = content.indexOf(key + "=[");
            if (idx == -1) return new HashSet<>();
            int start = content.indexOf('[', idx);
            int end   = content.indexOf(']', start);
            if (start == -1 || end == -1) return new HashSet<>();
            return Arrays.stream(content.substring(start + 1, end).split(","))
                    .map(s -> s.trim().replace("\"", "").toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    private Set<String> diff(Set<String> before, Set<String> after) {
        Set<String> added = new HashSet<>(after);
        added.removeAll(before);
        return added;
    }

    @Override
    public void stop() {
        if (watcherThread != null) watcherThread.interrupt();
        System.out.println("[✓] PermissionInterceptor Plugin detenido");
    }

    public PermissionEventCapture getEventCapture() { return eventCapture; }
    public NodeInfoProvider getNodeInfoProvider()    { return nodeInfoProvider; }
}
