package com.example.besu.plugin.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configurador del plugin
 * Lee configuración de archivo o propiedades del sistema
 */
public class PluginConfig {

    // Detectar ruta por defecto según el sistema
    private static final String DEFAULT_LOG_FILE = getDefaultLogPath();
    private static final String DEFAULT_METRICS_PORT = "9090";
    private static final boolean DEFAULT_ENABLE_REST_API = true;
    private static final boolean DEFAULT_ENABLE_METRICS = true;

    /**
     * Obtiene la ruta por defecto del log según dónde se ejecute Besu
     */
    private static String getDefaultLogPath() {
        String logDir = System.getProperty("besu.permission.log.dir");
        if (logDir != null && !logDir.isEmpty()) {
            return logDir + "/permission-events.log";
        }
        
        // Si estamos en /root/lnet/writer, usar esa ruta
        String besuHome = System.getProperty("besu.data.path");
        if (besuHome != null && besuHome.contains("lnet")) {
            return besuHome.replace("/data", "") + "/logs/permission-events.log";
        }
        
        // Default para sistemas Unix/Linux
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "C:\\besu\\logs\\permission-events.log";
        } else {
            return "/var/log/besu/permission-events.log";
        }
    }

    private String logFile;
    private int metricsPort;
    private boolean enableRestApi;
    private boolean enableMetrics;
    private boolean enableCsvExport;
    private int maxEventsInMemory;
    private String notificationWebhook;
    private String dataPath;
    private int rpcPort;
    private String rpcHost;

    // Blockchain audit-log integration
    private boolean blockchainEnabled;
    private String  blockchainContractAddress;
    private long    blockchainChainId;
    private String  blockchainNodeKeyPath;   // relative to data-path (or absolute)
    private String  blockchainRpcUrl;        // optional; default: http://127.0.0.1:${rpcPort}
    private long    blockchainGasPrice;
    private long    blockchainGasLimit;

    public PluginConfig() {
        this.logFile = DEFAULT_LOG_FILE;
        this.metricsPort = Integer.parseInt(DEFAULT_METRICS_PORT);
        this.enableRestApi = DEFAULT_ENABLE_REST_API;
        this.enableMetrics = DEFAULT_ENABLE_METRICS;
        this.enableCsvExport = true;
        this.maxEventsInMemory = 10000;
        this.notificationWebhook = null;
        this.dataPath = null;
        this.rpcPort = 0;
        this.rpcHost = "127.0.0.1";

        this.blockchainEnabled = false;
        this.blockchainContractAddress = null;
        this.blockchainChainId = 0L;
        this.blockchainNodeKeyPath = null;
        this.blockchainRpcUrl = null;
        this.blockchainGasPrice = 0L;
        this.blockchainGasLimit = 0L;
    }

    /**
     * Carga configuración desde archivo properties
     */
    public static PluginConfig loadFromFile(String filePath) throws IOException {
        PluginConfig config = new PluginConfig();
        Properties props = new Properties();

        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            props.load(Files.newInputStream(path));

            config.logFile = props.getProperty("log.file", DEFAULT_LOG_FILE);
            config.metricsPort = Integer.parseInt(
                    props.getProperty("metrics.port", DEFAULT_METRICS_PORT));
            config.enableRestApi = Boolean.parseBoolean(
                    props.getProperty("api.rest.enabled", "true"));
            config.enableMetrics = Boolean.parseBoolean(
                    props.getProperty("metrics.enabled", "true"));
            config.enableCsvExport = Boolean.parseBoolean(
                    props.getProperty("export.csv.enabled", "true"));
            config.maxEventsInMemory = Integer.parseInt(
                    props.getProperty("memory.max.events", "10000"));
            config.notificationWebhook = props.getProperty("notification.webhook", null);
            config.dataPath = props.getProperty("data.path", null);
            config.rpcPort  = Integer.parseInt(props.getProperty("rpc.port", "0"));
            config.rpcHost  = props.getProperty("rpc.host", "127.0.0.1");

            config.blockchainEnabled = Boolean.parseBoolean(
                    props.getProperty("blockchain.enabled", "false"));
            config.blockchainContractAddress = props.getProperty("blockchain.contract.address");
            String chainIdStr = props.getProperty("blockchain.chain.id");
            config.blockchainChainId = chainIdStr != null ? Long.parseLong(chainIdStr) : 0L;
            config.blockchainNodeKeyPath = props.getProperty("blockchain.node.key.path");
            config.blockchainRpcUrl = props.getProperty("blockchain.rpc.url");
            String gasPriceStr = props.getProperty("blockchain.gas.price");
            config.blockchainGasPrice = gasPriceStr != null ? Long.parseLong(gasPriceStr) : 0L;
            String gasLimitStr = props.getProperty("blockchain.gas.limit");
            config.blockchainGasLimit = gasLimitStr != null ? Long.parseLong(gasLimitStr) : 0L;

            System.out.println("[CONFIG] Configuración cargada desde: " + filePath);
        } else {
            System.out.println("[CONFIG] Archivo no encontrado, usando configuración por defecto");
        }

        return config;
    }

    /**
     * Carga configuración desde propiedades del sistema
     */
    public static PluginConfig loadFromSystemProperties() {
        PluginConfig config = new PluginConfig();

        config.logFile = System.getProperty("besu.permission.log.file", DEFAULT_LOG_FILE);
        config.metricsPort = Integer.parseInt(
                System.getProperty("besu.permission.metrics.port", DEFAULT_METRICS_PORT));
        config.enableRestApi = Boolean.parseBoolean(
                System.getProperty("besu.permission.api.rest.enabled", "true"));
        config.enableMetrics = Boolean.parseBoolean(
                System.getProperty("besu.permission.metrics.enabled", "true"));
        config.enableCsvExport = Boolean.parseBoolean(
                System.getProperty("besu.permission.export.csv.enabled", "true"));
        config.maxEventsInMemory = Integer.parseInt(
                System.getProperty("besu.permission.memory.max.events", "10000"));
        config.notificationWebhook = System.getProperty("besu.permission.notification.webhook");
        config.dataPath = System.getProperty("besu.permission.data.path", null);
        config.rpcPort  = Integer.parseInt(System.getProperty("besu.permission.rpc.port", "0"));
        config.rpcHost  = System.getProperty("besu.permission.rpc.host", "127.0.0.1");

        config.blockchainEnabled = Boolean.parseBoolean(
                System.getProperty("besu.permission.blockchain.enabled", "false"));
        config.blockchainContractAddress = System.getProperty("besu.permission.blockchain.contract.address");
        String chainIdSys = System.getProperty("besu.permission.blockchain.chain.id");
        config.blockchainChainId = chainIdSys != null ? Long.parseLong(chainIdSys) : 0L;
        config.blockchainNodeKeyPath = System.getProperty("besu.permission.blockchain.node.key.path");
        config.blockchainRpcUrl = System.getProperty("besu.permission.blockchain.rpc.url");
        String gasPriceSys = System.getProperty("besu.permission.blockchain.gas.price");
        config.blockchainGasPrice = gasPriceSys != null ? Long.parseLong(gasPriceSys) : 0L;
        String gasLimitSys = System.getProperty("besu.permission.blockchain.gas.limit");
        config.blockchainGasLimit = gasLimitSys != null ? Long.parseLong(gasLimitSys) : 0L;

        return config;
    }

    // Getters y Setters

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public int getMetricsPort() {
        return metricsPort;
    }

    public void setMetricsPort(int metricsPort) {
        this.metricsPort = metricsPort;
    }

    public boolean isEnableRestApi() {
        return enableRestApi;
    }

    public void setEnableRestApi(boolean enableRestApi) {
        this.enableRestApi = enableRestApi;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public boolean isEnableCsvExport() {
        return enableCsvExport;
    }

    public void setEnableCsvExport(boolean enableCsvExport) {
        this.enableCsvExport = enableCsvExport;
    }

    public int getMaxEventsInMemory() {
        return maxEventsInMemory;
    }

    public void setMaxEventsInMemory(int maxEventsInMemory) {
        this.maxEventsInMemory = maxEventsInMemory;
    }

    public String getNotificationWebhook() {
        return notificationWebhook;
    }

    public void setNotificationWebhook(String notificationWebhook) {
        this.notificationWebhook = notificationWebhook;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public int getRpcPort() { return rpcPort; }
    public void setRpcPort(int rpcPort) { this.rpcPort = rpcPort; }

    public String getRpcHost() { return rpcHost; }
    public void setRpcHost(String rpcHost) { this.rpcHost = rpcHost; }

    public boolean isBlockchainEnabled() { return blockchainEnabled; }
    public void setBlockchainEnabled(boolean v) { this.blockchainEnabled = v; }

    public String getBlockchainContractAddress() { return blockchainContractAddress; }
    public void setBlockchainContractAddress(String v) { this.blockchainContractAddress = v; }

    public long getBlockchainChainId() { return blockchainChainId; }
    public void setBlockchainChainId(long v) { this.blockchainChainId = v; }

    public String getBlockchainNodeKeyPath() { return blockchainNodeKeyPath; }
    public void setBlockchainNodeKeyPath(String v) { this.blockchainNodeKeyPath = v; }

    public String getBlockchainRpcUrl() { return blockchainRpcUrl; }
    public void setBlockchainRpcUrl(String v) { this.blockchainRpcUrl = v; }

    public long getBlockchainGasPrice() { return blockchainGasPrice; }
    public void setBlockchainGasPrice(long v) { this.blockchainGasPrice = v; }

    public long getBlockchainGasLimit() { return blockchainGasLimit; }
    public void setBlockchainGasLimit(long v) { this.blockchainGasLimit = v; }

    /**
     * Imprime la configuración
     */
    public void printConfig() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("BESU PERMISSION PLUGIN CONFIGURATION");
        System.out.println("═".repeat(70));
        System.out.println("Log File: " + logFile);
        System.out.println("Metrics Port: " + metricsPort);
        System.out.println("REST API Enabled: " + enableRestApi);
        System.out.println("Metrics Enabled: " + enableMetrics);
        System.out.println("CSV Export Enabled: " + enableCsvExport);
        System.out.println("Max Events in Memory: " + maxEventsInMemory);
        System.out.println("Notification Webhook: " + (notificationWebhook != null ? notificationWebhook : "disabled"));
        System.out.println("Blockchain Enabled:    " + blockchainEnabled);
        if (blockchainEnabled) {
            System.out.println("Blockchain Contract:   " + blockchainContractAddress);
            System.out.println("Blockchain Chain ID:   " + blockchainChainId);
            System.out.println("Blockchain Key Path:   " + blockchainNodeKeyPath);
            System.out.println("Blockchain RPC URL:    " + (blockchainRpcUrl != null ? blockchainRpcUrl : "(local: 127.0.0.1:" + rpcPort + ")"));
            System.out.println("Blockchain Gas Price:  " + blockchainGasPrice);
            System.out.println("Blockchain Gas Limit:  " + blockchainGasLimit);
        }
        System.out.println("═".repeat(70) + "\n");
    }

    @Override
    public String toString() {
        return "PluginConfig{" +
                "logFile='" + logFile + '\'' +
                ", metricsPort=" + metricsPort +
                ", enableRestApi=" + enableRestApi +
                ", enableMetrics=" + enableMetrics +
                ", enableCsvExport=" + enableCsvExport +
                ", maxEventsInMemory=" + maxEventsInMemory +
                ", notificationWebhook='" + notificationWebhook + '\'' +
                '}';
    }
}
