package servlets;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;

/**
 * GET /publish?topic=...&amp;message=... : publishes the message on the
 * given topic and returns a JSON snapshot of every topic's latest value.
 */
public class TopicDisplayer extends BaseServlet {

    /**
     * {@inheritDoc}
     * <p>
     * Publishes the {@code message} parameter on the {@code topic} parameter and returns a
     * JSON snapshot of every topic's latest value.
     *
     * @throws HTTPException {@code 400} if {@code topic} or {@code message} is missing,
     *         {@code 404} if the topic does not exist in the current graph
     */
    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        String topicName   = request.getParameters().get("topic");
        String messageText = request.getParameters().get("message");

        if (topicName == null || messageText == null) {
            throw new HTTPException(HTTPStatus.BAD_REQUEST, "Missing topic or message");
        }

        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();
        
        // Verify the topic actually exists in the graph BEFORE calling getTopic()
        // This prevents the TopicManager from accidentally auto-generating a missing node.
        boolean topicExists = false;
        for (Topic t : topicManager.getTopics()) {
            if (t.name.equals(topicName)) {
                topicExists = true;
                break;
            }
        }

        if (!topicExists) {
            throw new HTTPException(
                HTTPStatus.NOT_FOUND, 
                "Node '" + topicName + "' does not exist in the current graph."
            );
        }

        Topic topic = topicManager.getTopic(topicName);
        topic.publish(new Message(messageText));

        return sendJsonResponse(buildTopicsJson(topicManager));
    }

    /**
     * Builds the topics snapshot JSON: {@code {"topics":[{"name":..., "value":...}, ...]}}.
     *
     * @param topicManager the topic manager to read topics from
     * @return the JSON snapshot of every topic's latest value
     */
    private String buildTopicsJson(TopicManagerSingleton.TopicManager topicManager) {
        StringBuilder json = new StringBuilder("{\"topics\":[");
        boolean first = true;

        for (Topic t : topicManager.getTopics()) {
            if (!first) json.append(",");
            first = false;

            Message m = t.getLastMessage();
            String value = (m == null) ? "null"
                    : !Double.isNaN(m.asDouble) ? Double.toString(m.asDouble)
                    : m.asText;

            json.append("{")
                    .append("\"name\":\"").append(escapeJson(t.name)).append("\",")
                    .append("\"value\":\"").append(escapeJson(value)).append("\"")
                    .append("}");
        }

        return json.append("]}").toString();
    }

    /** Escapes double quotes so {@code s} can be embedded in a JSON string literal. */
    private String escapeJson(String s) {
        return s.replace("\"", "\\\"");
    }

    /** {@inheritDoc} */
    @Override
    public void close() {}
}