package com.example.besu.plugin.api;

import com.example.besu.plugin.PermissionEvent;
import com.example.besu.plugin.PermissionEventCapture;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio REST para exponer datos del plugin
 * Proporciona endpoints para consultar eventos capturados
 */
public class PermissionRestService {

    private final PermissionEventCapture eventCapture;
    private final ObjectMapper objectMapper;
    private final String logFile;

    public PermissionRestService(PermissionEventCapture eventCapture, String logFile) {
        this.eventCapture = eventCapture;
        this.logFile = logFile;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor para compatibilidad (sin logFile especificado)
     */
    public PermissionRestService(PermissionEventCapture eventCapture) {
        this(eventCapture, "/var/log/besu/permission-events.log");
    }

    /**
     * Obtiene todos los eventos capturados
     */
    public String getAllEvents() {
        try {
            List<Map<String, Object>> events = eventCapture.getEvents().stream()
                    .map(this::eventToMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("total", events.size());
            response.put("events", events);

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    /**
     * Obtiene eventos filtrados por tipo
     */
    public String getEventsByType(String type) {
        try {
            PermissionEvent.EventType eventType = PermissionEvent.EventType.valueOf(type.toUpperCase());
            List<Map<String, Object>> events = eventCapture.getEventsByType(eventType).stream()
                    .map(this::eventToMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("type", type);
            response.put("total", events.size());
            response.put("events", events);

            return objectMapper.writeValueAsString(response);
        } catch (IllegalArgumentException e) {
            return errorResponse("Tipo de evento inválido: " + type);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    /**
     * Obtiene estadísticas de eventos
     */
    public String getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("success", true);
            stats.put("total_events", eventCapture.getTotalEventsCount());
            stats.put("add_accounts_events", eventCapture.getEventsByType(
                    PermissionEvent.EventType.ADD_ACCOUNTS).size());
            stats.put("add_nodes_events", eventCapture.getEventsByType(
                    PermissionEvent.EventType.ADD_NODES).size());

            // Enodes únicos
            List<String> uniqueEnodes = eventCapture.getEvents().stream()
                    .map(PermissionEvent::getEnode)
                    .distinct()
                    .collect(Collectors.toList());
            stats.put("unique_enodes", uniqueEnodes.size());

            // Total de items permisionados
            int totalItems = eventCapture.getEvents().stream()
                    .mapToInt(e -> e.getAddresses().size())
                    .sum();
            stats.put("total_items_whitelisted", totalItems);

            return objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    /**
     * Obtiene eventos de las últimas N horas
     */
    public String getRecentEvents(int hours) {
        try {
            java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusHours(hours);

            List<Map<String, Object>> events = eventCapture.getEvents().stream()
                    .filter(e -> e.getTimestamp().isAfter(threshold))
                    .map(this::eventToMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hours", hours);
            response.put("since", threshold);
            response.put("total", events.size());
            response.put("events", events);

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    /**
     * Obtiene eventos por enode
     */
    public String getEventsByEnode(String enode) {
        try {
            List<Map<String, Object>> events = eventCapture.getEvents().stream()
                    .filter(e -> e.getEnode().equalsIgnoreCase(enode))
                    .map(this::eventToMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("enode", enode);
            response.put("total", events.size());
            response.put("events", events);

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    /**
     * Obtiene información resumida
     */
    public String getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("success", true);
            status.put("plugin_status", "ACTIVE");
            status.put("total_events_captured", eventCapture.getTotalEventsCount());
            status.put("log_file", logFile);
            status.put("version", "2.0.0");

            return objectMapper.writeValueAsString(status);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    /**
     * Convierte un evento al formato Map para JSON
     */
    private Map<String, Object> eventToMap(PermissionEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", event.getEventType().toString());
        map.put("method", event.getEventType().getMethodName());
        map.put("enode", event.getEnode());
        map.put("items", event.getAddresses());
        map.put("items_count", event.getAddresses().size());
        map.put("timestamp", event.getTimestamp().toString());
        return map;
    }

    /**
     * Formatea una respuesta de error
     */
    private String errorResponse(String message) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", message);
            return objectMapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"Error interno\"}";
        }
    }
}
