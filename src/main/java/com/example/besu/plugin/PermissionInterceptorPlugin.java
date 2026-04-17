package com.example.besu.plugin;

import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.BesuConfiguration;
import org.hyperledger.besu.plugin.services.RpcEndpointService;
import com.example.besu.plugin.config.PluginConfig;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

        System.out.println("[BESU PERMISSION RPC PLUGIN] Registrando plugin...");
    }

    @Override
    public void start() {
        // Resolve data path: plugin.properties > BesuConfiguration > default
        if (config.getDataPath() != null) {
            this.dataPath = Paths.get(config.getDataPath());
        } else {
            this.dataPath = besuContext.getService(BesuConfiguration.class)
                    .map(BesuConfiguration::getDataPath)
                    .orElse(Paths.get("."));
        }
        System.out.println("[BESU PERMISSION PLUGIN] data-path resuelto: " + dataPath.toAbsolutePath());

        boolean hasRpc = besuContext.getService(RpcEndpointService.class).isPresent();

        eventCapture.logStartup(
                config.getLogFile(),
                String.valueOf(config.getMetricsPort()),
                config.isEnableRestApi(),
                config.isEnableMetrics(),
                config.isEnableCsvExport(),
                config.getMaxEventsInMemory(),
                config.getNotificationWebhook() != null ? config.getNotificationWebhook() : "disabled",
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        if (hasRpc) {
            BesuConfiguration besuConfig = besuContext.getService(BesuConfiguration.class).orElse(null);
            // Always use 127.0.0.1 — getConfiguredRpcHttpHost() returns 0.0.0.0 (bind addr)
            int rpcPort = besuConfig != null ? besuConfig.getConfiguredRpcHttpPort() : 8545;
            startEnodeResolver("127.0.0.1", rpcPort);

            startPermissionsFileWatcher();
            System.out.println("[✓] PermissionInterceptor Plugin iniciado - monitoreando " + dataPath);
        } else {
            System.err.println("[✗] RpcEndpointService no disponible — RPC no habilitado en este nodo");
        }

        config.printConfig();
    }

    private void startEnodeResolver(String host, int port) {
        Thread t = new Thread(() -> {
            String url = "http://" + host + ":" + port + "/";
            String body = "{\"jsonrpc\":\"2.0\",\"method\":\"net_enode\",\"params\":[],\"id\":1}";
            ObjectMapper mapper = new ObjectMapper();
            for (int attempt = 1; attempt <= 15; attempt++) {
                try {
                    Thread.sleep(3000);
                    try (CloseableHttpClient client = HttpClients.createDefault()) {
                        HttpPost post = new HttpPost(url);
                        post.setEntity(new StringEntity(body));
                        post.setHeader("Content-Type", "application/json");
                        String response = client.execute(post, r -> EntityUtils.toString(r.getEntity()));
                        JsonNode root = mapper.readTree(response);
                        JsonNode result = root.path("result");
                        if (!result.isMissingNode() && !result.isNull()) {
                            String enode = result.asText();
                            nodeInfoProvider.setEnode(enode);
                            eventCapture.logLine("Local Node Enode: " + enode);
                            System.out.println("[✓] Enode resuelto: " + enode);
                            return;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    System.err.println("[WARN] Intento " + attempt + " obteniendo enode: " + e.getMessage());
                }
            }
        }, "enode-resolver");
        t.setDaemon(true);
        t.start();
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
