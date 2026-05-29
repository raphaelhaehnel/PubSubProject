package graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds agents from a JSON configuration file of the form:
 * <pre>
 * {
 *   "agents": [
 *     { "type": "PlusAgent", "subs": ["A", "B"], "pubs": ["C"] },
 *     { "type": "IncAgent",  "subs": ["C"],      "pubs": ["D"] }
 *   ]
 * }
 * </pre>
 * "type" must name a class in the graph package with a
 * <code>(String[] subs, String[] pubs)</code> constructor; it is loaded
 * by reflection so adding a new agent does not require changing this class.
 * Every agent is wrapped in a {@link ParallelAgent} so its callback runs
 * on its own thread.
 */
public class GenericConfig implements Config {

    private static final String AGENT_PACKAGE = "graph.";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String configPath;
    private final List<Agent> instantiatedAgents = new ArrayList<>();

    public GenericConfig() {
        // Start from a clean state every time a new configuration is built.
        TopicManagerSingleton.get().clear();
    }

    public void setConfFile(String configPath) {
        this.configPath = configPath;
    }

    public void create() {
        if (configPath == null) return;

        JsonNode root;
        try {
            root = MAPPER.readTree(new File(configPath));
        } catch (IOException e) {
            System.err.println("Failed to parse config JSON: " + e.getMessage());
            return;
        }

        JsonNode agents = root.get("agents");
        if (agents == null || !agents.isArray()) {
            System.err.println("Config must contain an \"agents\" array.");
            return;
        }

        for (JsonNode entry : agents) {
            String typeName = entry.path("type").asText(null);
            if (typeName == null) {
                System.err.println("Agent entry is missing \"type\" field, skipping.");
                continue;
            }

            String[] subs = readTopicArray(entry, "subs");
            String[] pubs = readTopicArray(entry, "pubs");
            String fullClassName = AGENT_PACKAGE + typeName;

            try {
                Class<?> agentClass = Class.forName(fullClassName);
                Constructor<?> constructor = agentClass.getConstructor(String[].class, String[].class);
                Agent agent = (Agent) constructor.newInstance((Object) subs, (Object) pubs);
                instantiatedAgents.add(new ParallelAgent(agent, 10));
            } catch (Exception e) {
                System.err.println("Could not load agent: " + fullClassName);
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getVersion() {
        return 1;
    }

    public void close() {
        for (Agent agent : instantiatedAgents) {
            agent.close();
        }
        instantiatedAgents.clear();
    }

    /** Reads a JSON array of strings into a String[] (empty if missing). */
    private static String[] readTopicArray(JsonNode entry, String field) {
        JsonNode node = entry.get(field);
        if (node == null || !node.isArray()) return new String[0];
        List<String> topics = new ArrayList<>();
        for (JsonNode t : node) topics.add(t.asText());
        return topics.toArray(new String[0]);
    }
}
