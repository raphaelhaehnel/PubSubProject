package view;

import graph.Graph;
import graph.Message;
import graph.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializes a {@link Graph} to the JSON format expected by Vis.js:
 * <pre>
 * { "nodes": [ { "id": 1, "label": "TA\n(5.0)", "color": "lightblue" }, ... ],
 *   "edges": [ { "from": 1, "to": 2 }, ... ] }
 * </pre>
 * Topic nodes (name starts with "T") are blue, agent nodes ("A") green.
 */
public class JsonGraphWriter {

    /**
     * Serializes the graph to the Vis.js JSON format. Each node gets a numeric id, a label
     * (its name without the {@code T}/{@code A} prefix, plus its latest value if any), and a
     * color; each edge is rendered as a {@code from}/{@code to} id pair.
     *
     * @param graph the graph to serialize
     * @return the JSON representation of the graph
     */
    public static String getGraphJSON(Graph graph) {
        StringBuilder json = new StringBuilder();

        json.append("{\"nodes\":[");

        // Vis.js needs numeric ids - we assign one per node as we discover it.
        Map<String, Integer> nodeIds = new HashMap<>();
        int id = 1;
        boolean first = true;

        for (Node node : graph) {
            if (!nodeIds.containsKey(node.getName())) {
                nodeIds.put(node.getName(), id++);

                if (!first) json.append(",");
                first = false;

                String color = node.getName().startsWith("T") ? "lightblue" : "lightgreen";

                String displayName = node.getName().length() > 1
                        ? node.getName().substring(1)
                        : node.getName();

                json.append("{")
                        .append("\"id\":").append(nodeIds.get(node.getName())).append(",")
                        .append("\"label\":\"").append(escapeJson(displayName));

                Message message = node.getMsg();
                if (message != null && message.asText != null) {
                    json.append("\\n(")
                            .append(escapeJson(message.asText))
                            .append(")");
                }

                json.append("\",");

                json.append("\"color\":\"").append(color).append("\"")
                        .append("}");
            }
        }

        json.append("],\"edges\":[");

        first = true;

        for (Node from : graph) {
            for (Node to : from.getEdges()) {
                if (!first) json.append(",");
                first = false;

                json.append("{")
                        .append("\"from\":").append(nodeIds.get(from.getName())).append(",")
                        .append("\"to\":").append(nodeIds.get(to.getName()))
                        .append("}");
            }
        }

        json.append("]}");

        return json.toString();
    }

    /** Escapes double quotes and newlines so {@code s} can be embedded in a JSON string. */
    private static String escapeJson(String s) {
        return s.replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
