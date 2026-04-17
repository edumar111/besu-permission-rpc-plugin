package com.example.besu.plugin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Representa un evento de cambio de permisos capturado
 */
public class PermissionEvent {

    public enum EventType {
        ADD_ACCOUNTS("perm_addAccountsToAllowlist"),
        ADD_NODES("perm_addNodesToAllowlist"),
        REMOVE_ACCOUNTS("perm_removeAccountsFromAllowlist"),
        REMOVE_NODES("perm_removeNodesFromAllowlist");

        private final String methodName;

        EventType(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }
    }

    private final EventType eventType;
    private final String enode;
    private final List<String> addresses;
    private final LocalDateTime timestamp;

    public PermissionEvent(EventType eventType, String enode, List<String> addresses, LocalDateTime timestamp) {
        this.eventType = Objects.requireNonNull(eventType, "eventType no puede ser null");
        this.enode = Objects.requireNonNull(enode, "enode no puede ser null");
        this.addresses = Objects.requireNonNull(addresses, "addresses no puede ser null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp no puede ser null");
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getEnode() {
        return enode;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PermissionEvent{" +
                "eventType=" + eventType +
                ", enode='" + enode + '\'' +
                ", addresses=" + addresses +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionEvent that = (PermissionEvent) o;
        return eventType == that.eventType &&
                Objects.equals(enode, that.enode) &&
                Objects.equals(addresses, that.addresses) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, enode, addresses, timestamp);
    }
}
