package view;

import graph.Graph;
import graph.Message;
import graph.Node;

import java.util.HashMap;
import java.util.Map;

public class HtmlGraphWriter {

    public static String getGraphJSON(Graph graph) {
        StringBuilder json = new StringBuilder();

        json.append("{\"nodes\":[");

        Map<String, Integer> nodeIds = new HashMap<>();
        int id = 1;
        boolean first = true;

        for (Node node : graph) {
            if (!nodeIds.containsKey(node.getName())) {
                nodeIds.put(node.getName(), id++);

                if (!first) json.append(",");
                first = false;

                String color = node.getName().startsWith("T") ? "lightblue" : "lightgreen";

                json.append("{")
                        .append("\"id\":").append(nodeIds.get(node.getName())).append(",")
                        .append("\"label\":\"").append(escapeJson(node.getName()));

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

    private static String escapeJson(String s) {
        return s.replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private static String escapeJS(String s) {
        return s.replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private static String getNodeTypeClass(String name) {
        if (name.startsWith("T")) return "topic";
        if (name.startsWith("A")) return "agent";
        return "";
    }
}