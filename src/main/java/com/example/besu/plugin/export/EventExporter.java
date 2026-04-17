package com.example.besu.plugin.export;

import com.example.besu.plugin.PermissionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exporta eventos capturados a diferentes formatos
 * Soporta: JSON, CSV, XML
 */
public class EventExporter {

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Exporta eventos a formato JSON
     */
    public static String exportToJson(List<PermissionEvent> events) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(events);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Exporta eventos a formato CSV
     * Formato: TYPE,ENODE,ITEMS,TIMESTAMP
     */
    public static String exportToCsv(List<PermissionEvent> events) {
        StringBuilder csv = new StringBuilder();
        csv.append("TYPE,ENODE,ITEMS,TIMESTAMP\n");

        for (PermissionEvent event : events) {
            csv.append(event.getEventType()).append(",");
            csv.append("\"").append(event.getEnode()).append("\",");
            csv.append("\"").append(String.join("|", event.getAddresses())).append("\",");
            csv.append(event.getTimestamp().format(formatter)).append("\n");
        }

        return csv.toString();
    }

    /**
     * Exporta eventos a formato XML
     */
    public static String exportToXml(List<PermissionEvent> events) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<permission-events>\n");
        xml.append("  <metadata>\n");
        xml.append("    <total>").append(events.size()).append("</total>\n");
        xml.append("    <export-time>").append(java.time.LocalDateTime.now()).append("</export-time>\n");
        xml.append("  </metadata>\n");
        xml.append("  <events>\n");

        for (PermissionEvent event : events) {
            xml.append("    <event>\n");
            xml.append("      <type>").append(escape(event.getEventType().toString())).append("</type>\n");
            xml.append("      <enode>").append(escape(event.getEnode())).append("</enode>\n");
            xml.append("      <items>\n");

            for (String item : event.getAddresses()) {
                xml.append("        <item>").append(escape(item)).append("</item>\n");
            }

            xml.append("      </items>\n");
            xml.append("      <timestamp>").append(event.getTimestamp().format(formatter))
                    .append("</timestamp>\n");
            xml.append("    </event>\n");
        }

        xml.append("  </events>\n");
        xml.append("</permission-events>\n");

        return xml.toString();
    }

    /**
     * Exporta eventos a formato TSV (Tab-Separated Values)
     */
    public static String exportToTsv(List<PermissionEvent> events) {
        StringBuilder tsv = new StringBuilder();
        tsv.append("TYPE\tENODE\tITEMS\tTIMESTAMP\n");

        for (PermissionEvent event : events) {
            tsv.append(event.getEventType()).append("\t");
            tsv.append(event.getEnode()).append("\t");
            tsv.append(String.join("|", event.getAddresses())).append("\t");
            tsv.append(event.getTimestamp().format(formatter)).append("\n");
        }

        return tsv.toString();
    }

    /**
     * Exporta eventos a formato HTML (tabla)
     */
    public static String exportToHtml(List<PermissionEvent> events) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <title>Permission Events Report</title>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <style>\n");
        html.append("    table { border-collapse: collapse; width: 100%; }\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
        html.append("    th { background-color: #4CAF50; color: white; }\n");
        html.append("    tr:nth-child(even) { background-color: #f2f2f2; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <h1>Permission Events Report</h1>\n");
        html.append("  <p>Generated: ").append(java.time.LocalDateTime.now()).append("</p>\n");
        html.append("  <p>Total Events: ").append(events.size()).append("</p>\n");
        html.append("  <table>\n");
        html.append("    <tr>\n");
        html.append("      <th>Type</th>\n");
        html.append("      <th>Enode</th>\n");
        html.append("      <th>Items</th>\n");
        html.append("      <th>Timestamp</th>\n");
        html.append("    </tr>\n");

        for (PermissionEvent event : events) {
            html.append("    <tr>\n");
            html.append("      <td>").append(event.getEventType()).append("</td>\n");
            html.append("      <td><code>").append(event.getEnode()).append("</code></td>\n");
            html.append("      <td>\n");
            for (String item : event.getAddresses()) {
                html.append("        <div>").append(escape(item)).append("</div>\n");
            }
            html.append("      </td>\n");
            html.append("      <td>").append(event.getTimestamp().format(formatter)).append("</td>\n");
            html.append("    </tr>\n");
        }

        html.append("  </table>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Escapa caracteres especiales para XML/HTML
     */
    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Genera un reporte de resumen
     */
    public static String generateSummaryReport(List<PermissionEvent> events) {
        StringBuilder report = new StringBuilder();
        report.append("═".repeat(80)).append("\n");
        report.append("PERMISSION EVENTS SUMMARY REPORT\n");
        report.append("═".repeat(80)).append("\n");
        report.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n");
        report.append("Total Events: ").append(events.size()).append("\n\n");

        // Por tipo
        report.append("Events by Type:\n");
        long addAccounts = events.stream()
                .filter(e -> e.getEventType() == PermissionEvent.EventType.ADD_ACCOUNTS)
                .count();
        long addNodes = events.stream()
                .filter(e -> e.getEventType() == PermissionEvent.EventType.ADD_NODES)
                .count();
        report.append("  - ADD_ACCOUNTS: ").append(addAccounts).append("\n");
        report.append("  - ADD_NODES: ").append(addNodes).append("\n");

        // Enodes únicos
        long uniqueEnodes = events.stream()
                .map(PermissionEvent::getEnode)
                .distinct()
                .count();
        report.append("\nUnique Enodes: ").append(uniqueEnodes).append("\n");

        // Total items
        int totalItems = events.stream()
                .mapToInt(e -> e.getAddresses().size())
                .sum();
        report.append("Total Items Whitelisted: ").append(totalItems).append("\n");

        // Promedio por evento
        double avgItems = totalItems > 0 ? (double) totalItems / events.size() : 0;
        report.append("Average Items per Event: ").append(String.format("%.2f", avgItems)).append("\n");

        report.append("\n").append("═".repeat(80)).append("\n");

        return report.toString();
    }
}
