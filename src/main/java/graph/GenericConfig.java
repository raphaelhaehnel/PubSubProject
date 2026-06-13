package graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import graph.agents.Agent;
import graph.agents.ParallelAgent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds agents from a JSON configuration file.
 * Now truly generic: expects the Fully Qualified Class Name (FQCN) in the "type" field
 * (e.g., "graph.agents.IncAgent"), but includes a legacy fallback for simple names.
 * * Every agent is wrapped in a {@link ParallelAgent} so its callback runs
 * on its own thread.
 */
public class GenericConfig implements Config {

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
            
            Class<?> agentClass = null;
            
            try {
                // Attempt 1: True Generic (e.g., "graph.agents.IncAgent" or "com.custom.MyAgent")
                agentClass = Class.forName(typeName);
                
            } catch (ClassNotFoundException e) {
                // Attempt 2: Legacy Fallback (If JSON just says "IncAgent")
                try {
                    agentClass = Class.forName("graph.agents." + typeName);
                } catch (ClassNotFoundException ex) {
                    System.err.println("CRITICAL: Could not locate agent class for type: " + typeName);
                    continue; // Skip this agent and move to the next one
                }
            }

            try {
                Constructor<?> constructor = agentClass.getConstructor(String[].class, String[].class);
                Agent agent = (Agent) constructor.newInstance((Object) subs, (Object) pubs);
                instantiatedAgents.add(new ParallelAgent(agent, 10));
            } catch (Exception e) {
                System.err.println("Failed to instantiate agent: " + (agentClass != null ? agentClass.getName() : typeName));
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