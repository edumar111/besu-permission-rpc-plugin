package com.example.besu.plugin;

import org.hyperledger.besu.plugin.ServiceManager;

/**
 * Proporciona información sobre el nodo actual
 */
public class NodeInfoProvider {

    private final ServiceManager besuContext;

    public NodeInfoProvider(ServiceManager besuContext) {
        this.besuContext = besuContext;
    }

    /**
     * Obtiene el enode del nodo actual
     * Formato: enode://publickey@ip:port
     */
    public String getNodeEnode() {
        try {
            // Intentar obtener información de nodos de Besu
            // El enode está disponible a través del servicio P2P
            return getEnodeFromContext();
        } catch (Exception e) {
            System.err.println("[ERROR] Obteniendo enode: " + e.getMessage());
            return "enode://unknown@localhost:30303";
        }
    }

    /**
     * Extrae el enode del contexto de Besu
     */
    private String getEnodeFromContext() {
        try {
            // En una implementación real, esto vendría del servicio P2P de Besu
            // Por ahora, retornamos un enode placeholder que será
            // reemplazado con el valor real del nodo
            String property = System.getProperty("besu.p2p.peer.id");
            String host = System.getProperty("besu.rpc.http.host", "127.0.0.1");
            String port = System.getProperty("besu.p2p.port", "30303");

            if (property != null) {
                return "enode://" + property + "@" + host + ":" + port;
            }

            return "enode://unknown@" + host + ":" + port;
        } catch (Exception e) {
            return "enode://unknown@localhost:30303";
        }
    }

    /**
     * Obtiene el network ID
     */
    public String getNetworkId() {
        return System.getProperty("besu.network.id", "unknown");
    }

    /**
     * Obtiene el puerto RPC HTTP
     */
    public int getRpcHttpPort() {
        try {
            return Integer.parseInt(System.getProperty("besu.rpc.http.port", "8545"));
        } catch (NumberFormatException e) {
            return 8545;
        }
    }

    /**
     * Obtiene el puerto P2P
     */
    public int getP2pPort() {
        try {
            return Integer.parseInt(System.getProperty("besu.p2p.port", "30303"));
        } catch (NumberFormatException e) {
            return 30303;
        }
    }
}
