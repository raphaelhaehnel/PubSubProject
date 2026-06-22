package graph;

import graph.agents.PlusAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GraphConfigTest {

    private TopicManagerSingleton.TopicManager topicManager;

    @BeforeEach
    void setUp() {
        topicManager = TopicManagerSingleton.get();
        topicManager.clear();
    }

    @Test
    void testGraphCreateFromTopicsAndDetectsNoCycles() {
        // Setup an acyclic pub-sub topology: Topic A -> PlusAgent -> Topic B
        PlusAgent agent = new PlusAgent(new String[]{"A"}, new String[]{"B"});
        
        Graph graph = new Graph();
        graph.createFromTopics();

        assertFalse(graph.isEmpty(), "Graph should populate nodes from the TopicManager.");
        assertFalse(graph.hasCycles(), "A linear flow should not contain cycles.");
        
        agent.close();
    }

    @Test
    void testGraphDetectsCyclesProperly() {
        // Create a deliberate cycle: A -> Agent1 -> B -> Agent2 -> A
        PlusAgent agent1 = new PlusAgent(new String[]{"A"}, new String[]{"B"});
        PlusAgent agent2 = new PlusAgent(new String[]{"B"}, new String[]{"A"});
        
        Graph graph = new Graph();
        graph.createFromTopics();
        
        assertTrue(graph.hasCycles(), "Graph must detect the cycle between the two agents and topics.");
        
        agent1.close();
        agent2.close();
    }

    @Test
    void testGenericConfigLoadsValidJson(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("valid_config.json");
        String jsonContent = """
        {
            "agents": [
                {
                    "type": "PlusAgent",
                    "subs": ["Input1", "Input2"],
                    "pubs": ["Output"]
                }
            ]
        }
        """;
        Files.writeString(configFile, jsonContent);

        GenericConfig config = new GenericConfig();
        config.setConfFile(configFile.toAbsolutePath().toString());
        
        assertDoesNotThrow(config::create, "Config should parse valid JSON without exceptions.");
        
        // Validate that the topics were initialized by the config parser
        assertNotNull(topicManager.getTopic("Input1"));
        assertNotNull(topicManager.getTopic("Output"));
        
        config.close();
    }

    @Test
    void testGenericConfigThrowsOnMissingAgentsArray(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("bad_config.json");
        String jsonContent = """
        {
            "wrong_key": []
        }
        """;
        Files.writeString(configFile, jsonContent);

        GenericConfig config = new GenericConfig();
        config.setConfFile(configFile.toAbsolutePath().toString());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, config::create);
        
        // We must check the cause's message because GenericConfig wraps exceptions in a generic catch block
        assertNotNull(exception.getCause(), "Expected the exception to wrap the original cause.");
        assertTrue(exception.getCause().getMessage().contains("must contain an \"agents\" array"));
    }

    @Test
    void testGenericConfigThrowsOnMissingTypeField(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("missing_type_config.json");
        String jsonContent = """
        {
            "agents": [
                {
                    "subs": ["A"],
                    "pubs": ["B"]
                }
            ]
        }
        """;
        Files.writeString(configFile, jsonContent);

        GenericConfig config = new GenericConfig();
        config.setConfFile(configFile.toAbsolutePath().toString());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, config::create);
        
        assertNotNull(exception.getCause(), "Expected the exception to wrap the original cause.");
        assertTrue(exception.getCause().getMessage().contains("missing the \"type\" field"));
    }

    @Test
    void testGenericConfigThrowsOnUnknownAgentType(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("unknown_agent.json");
        String jsonContent = """
        {
            "agents": [
                {
                    "type": "NonExistentAgent",
                    "subs": ["A"],
                    "pubs": ["B"]
                }
            ]
        }
        """;
        Files.writeString(configFile, jsonContent);

        GenericConfig config = new GenericConfig();
        config.setConfFile(configFile.toAbsolutePath().toString());

        // Expect an clear error, not a system crash
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, config::create);
        assertTrue(exception.getMessage().toLowerCase().contains("agent") || 
                   exception.getMessage().toLowerCase().contains("type"), 
                   "Should throw a descriptive error for unknown agent types.");
    }
}