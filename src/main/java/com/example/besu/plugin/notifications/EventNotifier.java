package com.example.besu.plugin.notifications;

import com.example.besu.plugin.PermissionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Sistema de notificaciones para eventos de permisos
 * Soporta: Webhooks HTTP, Slack, Discord, etc.
 */
public class EventNotifier {

    private final String webhookUrl;
    private final ObjectMapper objectMapper;

    public EventNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Notifica sobre un evento de permiso
     */
    public void notifyEvent(PermissionEvent event) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        try {
            sendWebhookNotification(event);
        } catch (Exception e) {
            System.err.println("[NOTIFIER] Error al enviar notificación: " + e.getMessage());
        }
    }

    /**
     * Envía notificación mediante webhook HTTP
     */
    private void sendWebhookNotification(PermissionEvent event) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // Crear payload
        Map<String, Object> payload = createPayload(event);
        String jsonPayload = objectMapper.writeValueAsString(payload);

        // Enviar datos
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Verificar respuesta
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            System.out.println("[NOTIFIER] Notificación enviada correctamente (código: " + responseCode + ")");
        } else {
            System.err.println("[NOTIFIER] Error en webhook (código: " + responseCode + ")");
        }

        connection.disconnect();
    }

    /**
     * Crea el payload para Slack
     */
    public String createSlackPayload(PermissionEvent event) throws Exception {
        Map<String, Object> payload = new HashMap<>();

        // Color según tipo
        String color = getColorByType(event.getEventType());

        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", color);
        attachment.put("title", "Permission Event: " + event.getEventType());
        attachment.put("fields", new Object[]{
                createSlackField("Type", event.getEventType().toString()),
                createSlackField("Enode", event.getEnode()),
                createSlackField("Items Count", String.valueOf(event.getAddresses().size())),
                createSlackField("Items", String.join("\n", event.getAddresses())),
                createSlackField("Timestamp", event.getTimestamp().toString())
        });

        payload.put("attachments", new Object[]{attachment});

        return objectMapper.writeValueAsString(payload);
    }

    /**
     * Crea el payload para Discord
     */
    public String createDiscordPayload(PermissionEvent event) throws Exception {
        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> embed = new HashMap<>();
        embed.put("title", "Permission Event Detected");
        embed.put("description", "Type: " + event.getEventType());
        embed.put("color", getColorCodeByType(event.getEventType()));

        Object[] fields = {
                createField("Enode", event.getEnode(), false),
                createField("Items Added", String.valueOf(event.getAddresses().size()), true),
                createField("Details", String.join("\n", event.getAddresses()), false),
                createField("Timestamp", event.getTimestamp().toString(), true)
        };

        embed.put("fields", fields);
        payload.put("embeds", new Object[]{embed});

        return objectMapper.writeValueAsString(payload);
    }

    /**
     * Crea el payload genérico
     */
    private Map<String, Object> createPayload(PermissionEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", event.getEventType().toString());
        payload.put("method", event.getEventType().getMethodName());
        payload.put("enode", event.getEnode());
        payload.put("items", event.getAddresses());
        payload.put("items_count", event.getAddresses().size());
        payload.put("timestamp", event.getTimestamp().toString());
        payload.put("server_timestamp", System.currentTimeMillis());

        return payload;
    }

    /**
     * Obtiene el color según el tipo de evento
     */
    private String getColorByType(PermissionEvent.EventType type) {
        return switch (type) {
            case ADD_ACCOUNTS -> "#36a64f";      // Verde
            case ADD_NODES -> "#0099cc";         // Azul
            case REMOVE_ACCOUNTS -> "#ff0000";   // Rojo
            case REMOVE_NODES -> "#ffaa00";      // Naranja
        };
    }

    /**
     * Obtiene el código de color numérico según el tipo
     */
    private Integer getColorCodeByType(PermissionEvent.EventType type) {
        return switch (type) {
            case ADD_ACCOUNTS -> 0x36a64f;       // Verde
            case ADD_NODES -> 0x0099cc;          // Azul
            case REMOVE_ACCOUNTS -> 0xff0000;    // Rojo
            case REMOVE_NODES -> 0xffaa00;       // Naranja
        };
    }

    /**
     * Crea un field para Slack
     */
    private Map<String, Object> createSlackField(String title, String value) {
        Map<String, Object> field = new HashMap<>();
        field.put("title", title);
        field.put("value", value);
        field.put("short", false);
        return field;
    }

    /**
     * Crea un field para Discord
     */
    private Map<String, Object> createField(String name, String value, boolean inline) {
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", inline);
        return field;
    }

    /**
     * Verifica si el webhook está configurado
     */
    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }
}
