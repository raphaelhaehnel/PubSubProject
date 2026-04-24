package servlets;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import server.RequestParser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TopicDisplayer implements Servlet {

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {

        try {
            String topicName = ri.getParameters().get("topic");
            String messageText = ri.getParameters().get("message");

            if (topicName == null || messageText == null) {
                sendResponse(toClient, 400,
                        "<html><body>Missing topic or message</body></html>");
                return;
            }

            TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();

            Topic topic = topicManager.getTopic(topicName);

            if (topic == null) {
                sendResponse(toClient, 404,
                        "<html><body>Topic not found</body></html>");
                return;
            }

            Message msg = new Message(messageText);
            topic.publish(msg);
            StringBuilder json = new StringBuilder();
            json.append("{\"topics\":[");

            boolean first = true;

            for (Topic t : topicManager.getTopics()) {
                if (!first) json.append(",");
                first = false;

                Message m = t.getLastMessage();

                String value;
                if (m == null) {
                    value = "null";
                } else if (!Double.isNaN(m.asDouble)) {
                    value = Double.toString(m.asDouble);
                } else {
                    value = m.asText;
                }

                json.append("{")
                        .append("\"name\":\"").append(escapeJson(t.name)).append("\",")
                        .append("\"value\":\"").append(escapeJson(value)).append("\"")
                        .append("}");
            }

            json.append("]}");

            sendJsonResponse(toClient, 200, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(toClient, 500,
                    "<html><body>Server error</body></html>");
        }
    }

    private String escapeJson(String s) {
        return s.replace("\"", "\\\"");
    }

    private void sendJsonResponse(OutputStream out, int statusCode, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String response =
                "HTTP/1.1 " + statusCode + " OK\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    @Override
    public void close() throws IOException {}

    private void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        String statusText = (statusCode == 200) ? "OK" : "ERROR";

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String response =
                "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n" +
                        "\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }
}