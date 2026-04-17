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
    private volatile String cachedEnode = null;

    public String getNodeEnode() {
        return cachedEnode != null ? cachedEnode : "enode://unknown@localhost:30303";
    }

    public void setEnode(String enode) {
        this.cachedEnode = enode;
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
