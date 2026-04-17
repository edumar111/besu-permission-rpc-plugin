package com.example.besu.plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

/**
 * Captura eventos de permisos y los registra en logs y memoria
 */
public class PermissionEventCapture {

    private final List<PermissionEvent> capturedEvents = new CopyOnWriteArrayList<>();
    private final List<PermissionEventListener> listeners = new CopyOnWriteArrayList<>();
    private String logFile;  // Configurable en lugar de static
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Constructor que permite especificar la ruta del log
     */
    public PermissionEventCapture(String logFilePath) {
        this.logFile = logFilePath;
    }

    /**
     * Constructor con ruta por defecto (para compatibilidad)
     */
    public PermissionEventCapture() {
        this("/var/log/besu/permission-events.log");
    }

    /**
     * Captura un evento de permiso para addAccountsToAllowlist
     */
    public void captureAccountPermissionEvent(String enode, List<String> addresses) {
        captureEvent(PermissionEvent.EventType.ADD_ACCOUNTS, enode, addresses);
    }

    /**
     * Captura un evento de permiso para addNodesToAllowlist
     */
    public void captureNodePermissionEvent(String enode, List<String> nodeEnodes) {
        captureEvent(PermissionEvent.EventType.ADD_NODES, enode, nodeEnodes);
    }

    public void captureAccountRemoveEvent(String enode, List<String> addresses) {
        captureEvent(PermissionEvent.EventType.REMOVE_ACCOUNTS, enode, addresses);
    }

    public void captureNodeRemoveEvent(String enode, List<String> nodeEnodes) {
        captureEvent(PermissionEvent.EventType.REMOVE_NODES, enode, nodeEnodes);
    }

    private void captureEvent(PermissionEvent.EventType type, String enode, List<String> items) {
        PermissionEvent event = new PermissionEvent(type, enode, items, LocalDateTime.now());
        capturedEvents.add(event);
        logToFile(event);
        notifyListeners(event);
        printEventToConsole(event);
    }

    private void notifyListeners(PermissionEvent event) {
        listeners.forEach(listener -> listener.onPermissionChanged(event));
    }

    private void printEventToConsole(PermissionEvent event) {
        String separator = "═".repeat(100);
        System.out.println("\n" + separator);
        System.out.println("┌─ [PERMISSION EVENT INTERCEPTED] " + event.getTimestamp());
        System.out.println("├─ TYPE: " + event.getEventType());
        System.out.println("├─ ENODE (Current Node): " + event.getEnode());
        System.out.println("├─ ITEMS ADDED: " + event.getAddresses().size());

        for (int i = 0; i < event.getAddresses().size(); i++) {
            String prefix = (i == event.getAddresses().size() - 1) ? "└─" : "├─";
            System.out.println(prefix + "  [" + (i + 1) + "] " + event.getAddresses().get(i));
        }
        System.out.println("└─ TIMESTAMP: " + event.getTimestamp());
        System.out.println(separator + "\n");
    }

    public void logStartup(String logFilePath, String metricsPort, boolean restApiEnabled,
                            boolean metricsEnabled, boolean csvEnabled, int maxEvents,
                            String webhook, String startTime) {
        String separator = "══════════════════════════════════════════════════════════════════════";
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(separator).append("\n");
        sb.append("BESU PERMISSION PLUGIN CONFIGURATION  [").append(startTime).append("]\n");
        sb.append(separator).append("\n");
        sb.append("Log File:              ").append(logFilePath).append("\n");
        sb.append("Metrics Port:          ").append(metricsPort).append("\n");
        sb.append("REST API Enabled:      ").append(restApiEnabled).append("\n");
        sb.append("Metrics Enabled:       ").append(metricsEnabled).append("\n");
        sb.append("CSV Export Enabled:    ").append(csvEnabled).append("\n");
        sb.append("Max Events in Memory:  ").append(maxEvents).append("\n");
        sb.append("Notification Webhook:  ").append(webhook).append("\n");
        sb.append("Local Node Enode:      resolving...\n");
        sb.append(separator).append("\n");
        writeToFile(sb.toString());
    }

    private void logToFile(PermissionEvent event) {
        String separator = "═".repeat(100);
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(separator).append("\n");
        sb.append("┌─ [PERMISSION EVENT INTERCEPTED] ").append(formatter.format(event.getTimestamp())).append("\n");
        sb.append("├─ TYPE: ").append(event.getEventType()).append("\n");
        sb.append("├─ ENODE (Current Node): ").append(event.getEnode()).append("\n");
        sb.append("├─ ITEMS ADDED: ").append(event.getAddresses().size()).append("\n");
        for (int i = 0; i < event.getAddresses().size(); i++) {
            String prefix = (i == event.getAddresses().size() - 1) ? "└─" : "├─";
            sb.append(prefix).append("  [").append(i + 1).append("] ")
              .append(event.getAddresses().get(i)).append("\n");
        }
        sb.append("└─ TIMESTAMP: ").append(formatter.format(event.getTimestamp())).append("\n");
        sb.append(separator).append("\n");
        writeToFile(sb.toString());
    }

    public void logLine(String line) {
        writeToFile(line + "\n");
    }

    private void writeToFile(String content) {
        try {
            Path logPath = Paths.get(logFile);
            Files.createDirectories(logPath.getParent());
            Files.write(logPath, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("[ERROR] Escribiendo log: " + e.getMessage());
        }
    }

    public List<PermissionEvent> getEvents() {
        return new ArrayList<>(capturedEvents);
    }

    public List<PermissionEvent> getEventsByType(PermissionEvent.EventType type) {
        return capturedEvents.stream()
                .filter(e -> e.getEventType() == type)
                .toList();
    }

    public void clearEvents() {
        capturedEvents.clear();
    }

    public int getTotalEventsCount() {
        return capturedEvents.size();
    }

    public void addListener(PermissionEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PermissionEventListener listener) {
        listeners.remove(listener);
    }
}
