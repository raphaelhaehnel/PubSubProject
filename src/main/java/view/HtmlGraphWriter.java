package view;

import graph.Graph;
import graph.Message;
import graph.Node;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HtmlGraphWriter {

    public static String getGraphHTML(Graph graph) {
        try {
            System.out.println("Running getGraphHTML");
            String html = new String(
                    Files.readAllBytes(Paths.get("html_files/graph.html")),
                    StandardCharsets.UTF_8
            );

            StringBuilder nodesBuilder = new StringBuilder();
            StringBuilder edgesBuilder = new StringBuilder();

            int id = 1;
            Map<String, Integer> nodeIds = new HashMap<>();

            for (Node node : graph) {
                if (!nodeIds.containsKey(node.getName())) {
                    nodeIds.put(node.getName(), id++);

                    String color = node.getName().startsWith("T") ? "lightblue" : "lightgreen";

                    nodesBuilder.append("{ id: ")
                            .append(nodeIds.get(node.getName()))
                            .append(", label: '")
                            .append(escapeJS(node.getName()));

                    Message message = node.getMsg();
                    if (message != null && message.asText != null) {
                        nodesBuilder.append("\\n(")
                                .append(escapeJS(message.asText))
                                .append(")");
                    }

                    nodesBuilder.append("'");



                    nodesBuilder.append(", color: '")
                            .append(color)
                            .append("' },\n");
                }
            }

            for (Node from : graph) {
                for (Node to : from.getEdges()) {
                    edgesBuilder.append("{ from: ")
                            .append(nodeIds.get(from.getName()))
                            .append(", to: ")
                            .append(nodeIds.get(to.getName()))
                            .append(" },\n");
                }
            }

            html = html.replace("<!-- NODES_PLACEHOLDER -->", nodesBuilder.toString());
            html = html.replace("<!-- EDGES_PLACEHOLDER -->", edgesBuilder.toString());

            return html;

        } catch (IOException e) {
            return "<html><body>Error loading graph</body></html>";
        }
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