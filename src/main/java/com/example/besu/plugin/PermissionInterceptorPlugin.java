package com.example.besu.plugin;

import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.RpcEndpointService;
import org.hyperledger.besu.plugin.services.rpc.PluginRpcResponse;
import com.example.besu.plugin.config.PluginConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.io.IOException;

/**
 * Plugin de Besu para interceptar llamadas RPC a perm_addAccountsToAllowlist
 * Captura automáticamente el enode del nodo y las direcciones permisionadas
 */
public class PermissionInterceptorPlugin implements BesuPlugin {

    private ServiceManager besuContext;
    private PermissionEventCapture eventCapture;
    private NodeInfoProvider nodeInfoProvider;
    private PluginConfig config;

    @Override
    public void register(final ServiceManager context) {
        this.besuContext = context;
        
        // Cargar configuración
        try {
            String configPath = System.getProperty("besu.permission.config");
            if (configPath != null) {
                this.config = PluginConfig.loadFromFile(configPath);
            } else {
                this.config = PluginConfig.loadFromSystemProperties();
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Cargando configuración: " + e.getMessage());
            this.config = new PluginConfig();
        }
        
        // Crear event capture con la ruta configurada
        this.eventCapture = new PermissionEventCapture(config.getLogFile());
        this.nodeInfoProvider = new NodeInfoProvider(context);

        System.out.println("\n" + "=".repeat(90));
        System.out.println("[BESU PERMISSION RPC PLUGIN] Registrando plugin...");
        System.out.println("=".repeat(90));
    }

    @Override
    public void start() {
        Optional<RpcEndpointService> rpcServiceOpt = besuContext.getService(RpcEndpointService.class);

        if (rpcServiceOpt.isPresent()) {
            RpcEndpointService rpcService = rpcServiceOpt.get();

            rpcService.registerRPCEndpoint("perm", "addAccountsToAllowlist", request -> {
                PluginRpcResponse response = rpcService.call("perm_addAccountsToAllowlist", request.getParams());
                try {
                    String enode = nodeInfoProvider.getNodeEnode();
                    List<String> addresses = extractItems(request.getParams());
                    eventCapture.captureAccountPermissionEvent(enode, addresses);
                } catch (Exception e) {
                    System.err.println("[ERROR] Capturando evento addAccountsToAllowlist: " + e.getMessage());
                }
                return response != null ? response.getResult() : null;
            });

            rpcService.registerRPCEndpoint("perm", "addNodesToAllowlist", request -> {
                PluginRpcResponse response = rpcService.call("perm_addNodesToAllowlist", request.getParams());
                try {
                    String enode = nodeInfoProvider.getNodeEnode();
                    List<String> nodes = extractItems(request.getParams());
                    eventCapture.captureNodePermissionEvent(enode, nodes);
                } catch (Exception e) {
                    System.err.println("[ERROR] Capturando evento addNodesToAllowlist: " + e.getMessage());
                }
                return response != null ? response.getResult() : null;
            });

            eventCapture.logStartup(
                    config.getLogFile(),
                    String.valueOf(config.getMetricsPort()),
                    config.isEnableRestApi(),
                    config.isEnableMetrics(),
                    config.isEnableCsvExport(),
                    config.getMaxEventsInMemory(),
                    config.getNotificationWebhook() != null ? config.getNotificationWebhook() : "disabled"
            );

            System.out.println("[✓] PermissionInterceptor Plugin iniciado correctamente");
            System.out.println("[✓] Interceptando: perm_addAccountsToAllowlist");
            System.out.println("[✓] Interceptando: perm_addNodesToAllowlist");
            System.out.println("[✓] Registros en: " + config.getLogFile());
            config.printConfig();
        } else {
            System.err.println("[✗] No se pudo obtener el servicio RPC");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractItems(Object[] params) {
        if (params == null || params.length == 0) return List.of();
        Object first = params[0];
        if (first instanceof String[]) return Arrays.asList((String[]) first);
        if (first instanceof List<?>) return (List<String>) first;
        if (first instanceof Collection<?>) return List.copyOf((Collection<String>) first);
        return List.of();
    }

    @Override
    public void stop() {
        System.out.println("[✓] PermissionInterceptor Plugin detenido");
    }

    public PermissionEventCapture getEventCapture() {
        return eventCapture;
    }

    public NodeInfoProvider getNodeInfoProvider() {
        return nodeInfoProvider;
    }
}
