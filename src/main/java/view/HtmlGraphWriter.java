package view;

import graph.Graph;
import graph.Node;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class HtmlGraphWriter {

    public static String getGraphHTML(Graph graph) {
        try {
            // Load HTML template
            String html = new String(
                    Files.readAllBytes(Paths.get("html_files/graph.html"))
            );

            StringBuilder content = new StringBuilder();

            content.append("<h3>Nodes</h3>");

            // ✅ Display all nodes
            for (Node node : graph) {
                String typeClass = getNodeTypeClass(node.getName());

                content.append("<div class='node " + typeClass + "'>");
                content.append(node.getName());
                content.append("</div>");
            }

            // ✅ Display edges
            content.append("<h3>Connections</h3>");

            for (Node from : graph) {
                for (Node to : from.getEdges()) {
                    content.append("<div>");
                    content.append(from.getName())
                            .append(" → ")
                            .append(to.getName());
                    content.append("</div>");
                }
            }

            // Replace placeholder
            html = html.replace("<!-- GRAPH_CONTENT -->", content.toString());

            return html;

        } catch (IOException e) {
            return "<html><body>Error loading graph</body></html>";
        }
    }

    private static String getNodeTypeClass(String name) {
        if (name.startsWith("T")) return "topic";
        if (name.startsWith("A")) return "agent";
        return "";
    }
}