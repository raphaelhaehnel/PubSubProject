package servlets;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import server.RequestParser;

import java.io.IOException;
import java.io.OutputStream;

public class TopicDisplayer extends BaseServlet {

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
        try {
            String topicName   = ri.getParameters().get("topic");
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

            topic.publish(new Message(messageText));

            String json = buildTopicsJson(topicManager);
            sendJsonResponse(toClient, json);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(toClient, 500, "<html><body>Server error</body></html>");
        }
    }

    private String buildTopicsJson(TopicManagerSingleton.TopicManager topicManager) {
        StringBuilder json = new StringBuilder("{\"topics\":[");
        boolean first = true;

        for (Topic t : topicManager.getTopics()) {
            if (!first) json.append(",");
            first = false;

            Message m = t.getLastMessage();
            String value = (m == null)              ? "null"
                    : !Double.isNaN(m.asDouble) ? Double.toString(m.asDouble)
                    : m.asText;

            json.append("{")
                    .append("\"name\":\"").append(escapeJson(t.name)).append("\",")
                    .append("\"value\":\"").append(escapeJson(value)).append("\"")
                    .append("}");
        }

        return json.append("]}").toString();
    }

    private String escapeJson(String s) {
        return s.replace("\"", "\\\"");
    }

    @Override
    public void close() {}
}
