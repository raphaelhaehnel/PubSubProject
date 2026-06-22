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

public class GenericConfig implements Config {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_PACKAGE = "graph.agents.";

    private String configPath;
    private final List<Agent> instantiatedAgents = new ArrayList<>();

    public GenericConfig() {
        TopicManagerSingleton.get().clear();
    }

    public void setConfFile(String configPath) {
        this.configPath = configPath;
    }

    public void create() {
        if (configPath == null) return;

        try {
            JsonNode root = MAPPER.readTree(new File(configPath));
            JsonNode agents = root.get("agents");
            
            if (agents == null || !agents.isArray()) {
                throw new IllegalArgumentException("Config must contain an \"agents\" array.");
            }

            for (JsonNode entry : agents) {
                String typeName = entry.path("type").asText(null);
                if (typeName == null || typeName.isBlank()) {
                    throw new IllegalArgumentException("An agent entry is missing the \"type\" field.");
                }

                String[] subs = readTopicArray(entry, "subs");
                String[] pubs = readTopicArray(entry, "pubs");
                
                Class<?> agentClass = resolveAgentClass(typeName);
                Constructor<?> constructor = agentClass.getConstructor(String[].class, String[].class);
                Agent agent = (Agent) constructor.newInstance((Object) subs, (Object) pubs);
                instantiatedAgents.add(new ParallelAgent(agent, 10));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse config JSON", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate graph agents", e);
        }
    }

    private Class<?> resolveAgentClass(String typeName) {
        try {
            // If it contains a dot, assume it's a fully qualified name. Otherwise, use default.
            String fullPath = typeName.contains(".") ? typeName : DEFAULT_PACKAGE + typeName;
            return Class.forName(fullPath);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not locate agent class for type: " + typeName, e);
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

    private static String[] readTopicArray(JsonNode entry, String field) {
        JsonNode node = entry.get(field);
        if (node == null || !node.isArray()) return new String[0];
        
        List<String> topics = new ArrayList<>();
        for (JsonNode t : node) topics.add(t.asText());
        return topics.toArray(new String[0]);
    }
}