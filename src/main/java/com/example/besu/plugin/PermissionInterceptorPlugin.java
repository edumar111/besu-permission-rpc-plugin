package com.example.besu.plugin;

import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.BesuConfiguration;
import org.hyperledger.besu.plugin.services.RpcEndpointService;
import com.example.besu.plugin.config.PluginConfig;
import com.example.besu.plugin.blockchain.BlockchainReporter;
import com.example.besu.plugin.blockchain.ContractTxSender;
import com.example.besu.plugin.blockchain.NodeKeyLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;

import java.math.BigInteger;

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

    private static final Logger LOG = LogManager.getLogger(PermissionInterceptorPlugin.class);

    private ServiceManager besuContext;
    private PermissionEventCapture eventCapture;
    private NodeInfoProvider nodeInfoProvider;
    private PluginConfig config;
    private Path dataPath;
    private volatile Thread watcherThread;
    private BlockchainReporter blockchainReporter;

    @Override
    public void register(final ServiceManager context) {
        this.besuContext = context;

        try {
            String configPath = System.getProperty("besu.permission.config");
            this.config = configPath != null
                    ? PluginConfig.loadFromFile(configPath)
                    : PluginConfig.loadFromSystemProperties();
        } catch (IOException e) {
            LOG.error("[BESU PERMISSION PLUGIN] Error cargando configuración: {}", e.getMessage());
            this.config = new PluginConfig();
        }

        this.eventCapture = new PermissionEventCapture(config.getLogFile());
        this.nodeInfoProvider = new NodeInfoProvider(context);

        LOG.info("[BESU PERMISSION PLUGIN] Registrando plugin...");
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
        LOG.info("[BESU PERMISSION PLUGIN] data-path resuelto: {}", dataPath.toAbsolutePath());

        boolean hasRpc = besuContext.getService(RpcEndpointService.class).isPresent();

        eventCapture.logStartup(
                config.getLogFile(),
                config.getMaxEventsInMemory(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        if (hasRpc) {
            // Port resolution: plugin.properties rpc.port > BesuConfiguration > error
            int rpcPort;
            if (config.getRpcPort() > 0) {
                rpcPort = config.getRpcPort();
            } else {
                rpcPort = besuContext.getService(BesuConfiguration.class)
                        .map(BesuConfiguration::getConfiguredRpcHttpPort)
                        .orElse(0);
            }
            if (rpcPort <= 0) {
                LOG.error("[BESU PERMISSION PLUGIN] rpc.port no configurado — agrega 'rpc.port=XXXX' en plugin.properties");
            } else {
                // Always connect to 127.0.0.1 — bind addr (0.0.0.0) no es usable para outbound
                startEnodeResolver("127.0.0.1", rpcPort);
            }

            startPermissionsFileWatcher();

            if (config.isBlockchainEnabled()) {
                startBlockchainReporter(rpcPort);
            } else {
                LOG.info("[BLOCKCHAIN] Reporter deshabilitado (blockchain.enabled=false)");
            }

            LOG.info("[BESU PERMISSION PLUGIN] PermissionInterceptor Plugin iniciado - monitoreando {}", dataPath);
        } else {
            LOG.error("[BESU PERMISSION PLUGIN] RpcEndpointService no disponible — RPC no habilitado en este nodo");
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
                            LOG.info("[BESU PERMISSION PLUGIN] Enode resuelto: {}", enode);
                            return;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    LOG.warn("[BESU PERMISSION PLUGIN] Intento {} obteniendo enode: {}", attempt, e.getMessage());
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

                LOG.info("[BESU PERMISSION PLUGIN] Watcheando permisos en: {}", permFile);

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watcher.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                        Path changed = (Path) event.context();
                        if (!changed.getFileName().toString().equals("permissions_config.toml")) continue;

                        Thread.sleep(150); // wait for Besu to finish writing

                        String enode = nodeInfoProvider.getNodeEnode();

                        Set<String> newAccounts = readAllowlist(permFile, "accounts-allowlist");
                        Set<String> addedAccounts   = diff(knownAccounts, newAccounts);
                        Set<String> removedAccounts = diff(newAccounts, knownAccounts);
                        if (!addedAccounts.isEmpty())
                            eventCapture.captureAccountPermissionEvent(enode, new ArrayList<>(addedAccounts));
                        if (!removedAccounts.isEmpty())
                            eventCapture.captureAccountRemoveEvent(enode, new ArrayList<>(removedAccounts));
                        knownAccounts = newAccounts;

                        Set<String> newNodes = readAllowlist(permFile, "nodes-allowlist");
                        Set<String> addedNodes   = diff(knownNodes, newNodes);
                        Set<String> removedNodes = diff(newNodes, knownNodes);
                        if (!addedNodes.isEmpty())
                            eventCapture.captureNodePermissionEvent(enode, new ArrayList<>(addedNodes));
                        if (!removedNodes.isEmpty())
                            eventCapture.captureNodeRemoveEvent(enode, new ArrayList<>(removedNodes));
                        knownNodes = newNodes;
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.error("[BESU PERMISSION PLUGIN] Watcher de permisos: {}", e.getMessage());
            }
        }, "permission-file-watcher");

        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void startBlockchainReporter(int rpcPort) {
        try {
            List<String> missing = new ArrayList<>();
            if (config.getBlockchainContractAddress() == null || config.getBlockchainContractAddress().isBlank())
                missing.add("blockchain.contract.address");
            if (config.getBlockchainChainId() <= 0)
                missing.add("blockchain.chain.id");
            if (config.getBlockchainNodeKeyPath() == null || config.getBlockchainNodeKeyPath().isBlank())
                missing.add("blockchain.node.key.path");
            if (config.getBlockchainGasLimit() <= 0)
                missing.add("blockchain.gas.limit");
            if (!missing.isEmpty()) {
                String msg = "[BLOCKCHAIN ERROR] Propiedades requeridas faltantes en plugin.properties: " + missing;
                LOG.error(msg);
                eventCapture.logLine(msg);
                return;
            }

            Path keyPath = Paths.get(config.getBlockchainNodeKeyPath());
            if (!keyPath.isAbsolute()) {
                keyPath = dataPath.resolve(keyPath);
            }
            Credentials credentials = NodeKeyLoader.load(keyPath);

            String rpcUrl = config.getBlockchainRpcUrl();
            if (rpcUrl == null || rpcUrl.isBlank()) {
                rpcUrl = "http://127.0.0.1:" + rpcPort + "/";
            }

            ContractTxSender sender = new ContractTxSender(
                    rpcUrl,
                    config.getBlockchainChainId(),
                    BigInteger.valueOf(config.getBlockchainGasPrice()),
                    BigInteger.valueOf(config.getBlockchainGasLimit()),
                    config.getBlockchainContractAddress(),
                    credentials);

            blockchainReporter = new BlockchainReporter(sender, eventCapture);
            eventCapture.addListener(blockchainReporter);
            blockchainReporter.start();

            LOG.info("[BLOCKCHAIN] Reporter iniciado. Sender={} contract={} chainId={} rpc={}",
                    sender.getSenderAddress(),
                    config.getBlockchainContractAddress(),
                    config.getBlockchainChainId(),
                    rpcUrl);
        } catch (Exception e) {
            LOG.error("[BLOCKCHAIN ERROR] No se pudo iniciar blockchain reporter: {}", e.getMessage());
            eventCapture.logLine("[BLOCKCHAIN ERROR] init failed: " + e.getMessage());
        }
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
        if (blockchainReporter != null) blockchainReporter.stop();
        LOG.info("[BESU PERMISSION PLUGIN] PermissionInterceptor Plugin detenido");
    }

    public PermissionEventCapture getEventCapture() { return eventCapture; }
    public NodeInfoProvider getNodeInfoProvider()    { return nodeInfoProvider; }
}
