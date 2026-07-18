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
 * Reflection-based implementation of {@link Config} that builds the computational
 * graph from a JSON configuration file.
 * <p>
 * Each entry in the config's {@code "agents"} array names an agent {@code type} plus
 * its {@code subs} (input topics) and {@code pubs} (output topics). The class loads
 * the matching agent class by name (defaulting to the {@code graph.agents} package),
 * instantiates it through its {@code (String[] subs, String[] pubs)} constructor, and
 * wraps every agent in a {@link ParallelAgent} so callbacks run off the publishing thread.
 */
public class GenericConfig implements Config {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_PACKAGE = "graph.agents.";

    private String configPath;
    private final List<Agent> instantiatedAgents = new ArrayList<>();

    /**
     * Creates an empty configuration and clears every existing topic, so the graph
     * is built on a clean {@link TopicManagerSingleton} state.
     */
    public GenericConfig() {
        TopicManagerSingleton.get().clear();
    }

    /**
     * Sets the path of the JSON file to read when {@link #create()} is called.
     *
     * @param configPath filesystem path to the configuration file
     */
    public void setConfFile(String configPath) {
        this.configPath = configPath;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reads the configured JSON file, instantiates every declared agent via reflection,
     * and wraps each one in a {@link ParallelAgent}. Does nothing if no config file was set.
     *
     * @throws IllegalArgumentException if the file cannot be parsed, the {@code "agents"}
     *         array is missing, an entry lacks a {@code "type"}, or an agent cannot be instantiated
     */
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

    /**
     * Resolves an agent {@code type} name to a {@link Class}. A name containing a dot is
     * treated as fully qualified; otherwise it is looked up under {@value #DEFAULT_PACKAGE}.
     *
     * @param typeName the agent type name from the config
     * @return the resolved agent class
     * @throws IllegalArgumentException if no class matches the given type
     */
    private Class<?> resolveAgentClass(String typeName) {
        try {
            // If it contains a dot, assume it's a fully qualified name. Otherwise, use default.
            String fullPath = typeName.contains(".") ? typeName : DEFAULT_PACKAGE + typeName;
            return Class.forName(fullPath);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not locate agent class for type: " + typeName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /** {@inheritDoc} */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Closes every agent created by {@link #create()} (unsubscribing them from their
     * topics) and forgets them, so the configuration can be rebuilt cleanly.
     */
    public void close() {
        for (Agent agent : instantiatedAgents) {
            agent.close();
        }
        instantiatedAgents.clear();
    }

    /**
     * Reads a topic-name array (e.g. {@code "subs"} or {@code "pubs"}) from a JSON agent entry.
     *
     * @param entry the JSON object describing one agent
     * @param field the array field to read
     * @return the topic names, or an empty array if the field is absent or not an array
     */
    private static String[] readTopicArray(JsonNode entry, String field) {
        JsonNode node = entry.get(field);
        if (node == null || !node.isArray()) return new String[0];
        
        List<String> topics = new ArrayList<>();
        for (JsonNode t : node) topics.add(t.asText());
        return topics.toArray(new String[0]);
    }
}