package servlets;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;


/**
 * GET /publish?topic=...&message=... : publishes the message on the
 * given topic and returns a JSON snapshot of every topic's latest value.
 */
public class TopicDisplayer extends BaseServlet {

    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        String topicName   = request.getParameters().get("topic");
        String messageText = request.getParameters().get("message");

        if (topicName == null || messageText == null) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "Missing topic or message");
        }

        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();
        Topic topic = topicManager.getTopic(topicName);

        topic.publish(new Message(messageText));

        return sendJsonResponse(buildTopicsJson(topicManager));
    }

    /** Builds {"topics":[{"name":..., "value":...}, ...]}. */
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
