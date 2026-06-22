package servlets;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import graph.agents.Agent;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.exceptions.HTTPException;

import java.util.HashSet;
import java.util.Set;

/**
 * POST /reset : calls reset() on every agent then publishes "0" on every
 * topic, so the whole system goes back to a clean zeroed state.
 * Returns the same JSON snapshot as {@link TopicDisplayer}.
 */
public class ResetServlet extends BaseServlet {

    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();

        // An agent can appear on several topics, so we deduplicate
        // before calling reset().
        Set<Agent> allAgents = new HashSet<>();
        for (Topic t : topicManager.getTopics()) {
            allAgents.addAll(t.getSubscribers());
            allAgents.addAll(t.getPublishers());
        }

        for (Agent a : allAgents) {
            a.reset();
        }

        Message zero = new Message("0");
        for (Topic t : topicManager.getTopics()) {
            t.publish(zero);
        }

        return sendJsonResponse(buildTopicsJson(topicManager));
    }

    /** Same shape as {@link TopicDisplayer#buildTopicsJson}. */
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
