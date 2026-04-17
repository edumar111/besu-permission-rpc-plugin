package com.example.besu.plugin.metrics;

import com.example.besu.plugin.PermissionEvent;
import com.example.besu.plugin.PermissionEventCapture;

import java.util.HashMap;
import java.util.Map;

/**
 * Proveedor de métricas compatible con Prometheus
 * Permite monitorear el plugin con sistemas como Prometheus y Grafana
 */
public class MetricsProvider {

    private final PermissionEventCapture eventCapture;
    private long startTime;

    public MetricsProvider(PermissionEventCapture eventCapture) {
        this.eventCapture = eventCapture;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Obtiene las métricas en formato Prometheus
     * Compatible con: Prometheus, Grafana, etc.
     */
    public String getPrometheusMetrics() {
        StringBuilder metrics = new StringBuilder();

        // Header
        metrics.append("# HELP besu_permission_events_total Número total de eventos capturados\n");
        metrics.append("# TYPE besu_permission_events_total counter\n");
        metrics.append("besu_permission_events_total ").append(eventCapture.getTotalEventsCount()).append("\n\n");

        // Eventos por tipo
        metrics.append("# HELP besu_permission_events_by_type Número de eventos por tipo\n");
        metrics.append("# TYPE besu_permission_events_by_type gauge\n");
        metrics.append("besu_permission_events_by_type{type=\"ADD_ACCOUNTS\"} ")
                .append(eventCapture.getEventsByType(PermissionEvent.EventType.ADD_ACCOUNTS).size()).append("\n");
        metrics.append("besu_permission_events_by_type{type=\"ADD_NODES\"} ")
                .append(eventCapture.getEventsByType(PermissionEvent.EventType.ADD_NODES).size()).append("\n");
        metrics.append("besu_permission_events_by_type{type=\"REMOVE_ACCOUNTS\"} ")
                .append(eventCapture.getEventsByType(PermissionEvent.EventType.REMOVE_ACCOUNTS).size()).append("\n");
        metrics.append("besu_permission_events_by_type{type=\"REMOVE_NODES\"} ")
                .append(eventCapture.getEventsByType(PermissionEvent.EventType.REMOVE_NODES).size()).append("\n\n");

        // Items whitelisted
        int totalItems = eventCapture.getEvents().stream()
                .mapToInt(e -> e.getAddresses().size())
                .sum();
        metrics.append("# HELP besu_permission_items_whitelisted Total de items en whitelist\n");
        metrics.append("# TYPE besu_permission_items_whitelisted gauge\n");
        metrics.append("besu_permission_items_whitelisted ").append(totalItems).append("\n\n");

        // Uptime
        long uptime = System.currentTimeMillis() - startTime;
        metrics.append("# HELP besu_permission_plugin_uptime_seconds Tiempo de ejecución en segundos\n");
        metrics.append("# TYPE besu_permission_plugin_uptime_seconds gauge\n");
        metrics.append("besu_permission_plugin_uptime_seconds ").append(uptime / 1000).append("\n");

        return metrics.toString();
    }

    /**
     * Obtiene las métricas en formato JSON
     */
    public String getJsonMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("timestamp", System.currentTimeMillis());
        metrics.put("total_events", eventCapture.getTotalEventsCount());
        metrics.put("uptime_seconds", (System.currentTimeMillis() - startTime) / 1000);

        Map<String, Integer> eventsByType = new HashMap<>();
        eventsByType.put("ADD_ACCOUNTS", eventCapture.getEventsByType(
                PermissionEvent.EventType.ADD_ACCOUNTS).size());
        eventsByType.put("ADD_NODES", eventCapture.getEventsByType(
                PermissionEvent.EventType.ADD_NODES).size());
        eventsByType.put("REMOVE_ACCOUNTS", eventCapture.getEventsByType(
                PermissionEvent.EventType.REMOVE_ACCOUNTS).size());
        eventsByType.put("REMOVE_NODES", eventCapture.getEventsByType(
                PermissionEvent.EventType.REMOVE_NODES).size());

        metrics.put("events_by_type", eventsByType);

        int totalItems = eventCapture.getEvents().stream()
                .mapToInt(e -> e.getAddresses().size())
                .sum();
        metrics.put("total_items_whitelisted", totalItems);

        long uniqueEnodes = eventCapture.getEvents().stream()
                .map(PermissionEvent::getEnode)
                .distinct()
                .count();
        metrics.put("unique_enodes", uniqueEnodes);

        return formatJson(metrics);
    }

    /**
     * Obtiene un resumen de métricas
     */
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();

        summary.put("total_events", eventCapture.getTotalEventsCount());
        summary.put("add_accounts", eventCapture.getEventsByType(
                PermissionEvent.EventType.ADD_ACCOUNTS).size());
        summary.put("add_nodes", eventCapture.getEventsByType(
                PermissionEvent.EventType.ADD_NODES).size());
        summary.put("remove_accounts", eventCapture.getEventsByType(
                PermissionEvent.EventType.REMOVE_ACCOUNTS).size());
        summary.put("remove_nodes", eventCapture.getEventsByType(
                PermissionEvent.EventType.REMOVE_NODES).size());

        int totalItems = eventCapture.getEvents().stream()
                .mapToInt(e -> e.getAddresses().size())
                .sum();
        summary.put("total_whitelisted_items", totalItems);

        summary.put("uptime_seconds", (System.currentTimeMillis() - startTime) / 1000);

        return summary;
    }

    /**
     * Formatea un map como JSON simple
     */
    private String formatJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{\n");
        map.forEach((key, value) -> {
            json.append("  \"").append(key).append("\": ");
            if (value instanceof Map) {
                json.append(formatJson((Map<String, Object>) value));
            } else if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            json.append(",\n");
        });
        json.setLength(json.length() - 2); // Remover última coma
        json.append("\n}");
        return json.toString();
    }
}
