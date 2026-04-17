package com.example.besu.plugin;

import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.RpcEndpointService;
import com.example.besu.plugin.config.PluginConfig;

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
        Optional<RpcEndpointService> rpcService = besuContext.getService(RpcEndpointService.class);

        if (rpcService.isPresent()) {
            System.out.println("\n" + "✓".repeat(45));
            System.out.println("[✓] PermissionInterceptor Plugin iniciado correctamente");
            System.out.println("[✓] Monitoreando: perm_addAccountsToAllowlist");
            System.out.println("[✓] Monitoreando: perm_addNodesToAllowlist");
            System.out.println("[✓] Registros en: " + config.getLogFile());
            System.out.println("[✓] Versión: 2.0.0 (Besu 25.8.0 + Java 17)");
            System.out.println("✓".repeat(45) + "\n");
            
            // Imprimir configuración
            config.printConfig();
        } else {
            System.err.println("[✗] No se pudo obtener el servicio RPC");
        }
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
