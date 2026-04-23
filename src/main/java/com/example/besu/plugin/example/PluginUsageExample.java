package com.example.besu.plugin.example;

import com.example.besu.plugin.PermissionEvent;
import com.example.besu.plugin.PermissionEventListener;
import com.example.besu.plugin.PermissionInterceptorPlugin;

/**
 * Ejemplo de cómo integrar el plugin en tu aplicación
 * 
 * Este ejemplo muestra cómo:
 * 1. Obtener una instancia del plugin
 * 2. Escuchar eventos de permisos
 * 3. Procesar eventos en tiempo real
 */
public class PluginUsageExample {

    public static void main(String[] args) {
        // En una aplicación real, obtendrías el plugin desde BesuContext
        PermissionInterceptorPlugin plugin = new PermissionInterceptorPlugin();
        
        // Ejemplo 1: Agregar un listener para eventos de cuentas
        plugin.getEventCapture().addListener(new PermissionEventListener() {
            @Override
            public void onPermissionChanged(PermissionEvent event) {
                if (event.getEventType() == PermissionEvent.EventType.ADD_ACCOUNTS) {
                    System.out.println("Nueva cuenta permisionada!");
                    System.out.println("Enode: " + event.getEnode());
                    System.out.println("Cuentas: " + event.getAddresses());
                }
            }
        });
        
        // Ejemplo 2: Usar lambda expressions (Java 8+)
        plugin.getEventCapture().addListener(event -> {
            System.out.println("[LOG] Evento capturado: " + event.getEventType());
            System.out.println("[LOG] Timestamp: " + event.getTimestamp());
        });
        
        // Ejemplo 3: Filtrar eventos por tipo
        plugin.getEventCapture().addListener(event -> {
            if (event.getEventType() == PermissionEvent.EventType.ADD_NODES) {
                handleNodeAddition(event);
            }
        });
    }
    
    /**
     * Procesa eventos de adición de nodos
     */
    private static void handleNodeAddition(PermissionEvent event) {
        System.out.println("Nuevos nodos agregados al allowlist:");
        for (String nodeEnode : event.getAddresses()) {
            System.out.println("  - " + nodeEnode);
        }
        
        // Aquí puedes conectar a los nuevos nodos
        connectToNodes(event.getAddresses());
    }
    
    /**
     * Conecta a nuevos nodos (ejemplo)
     */
    private static void connectToNodes(java.util.List<String> nodeEnodes) {
        for (String enode : nodeEnodes) {
            System.out.println("[P2P] Conectando a: " + enode);
            // Aquí iría la lógica de conexión real
        }
    }
}
