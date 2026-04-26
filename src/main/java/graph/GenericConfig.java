package graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GenericConfig implements Config {

    private String configPath;

    private static final Pattern TOPIC_PATTERN = Pattern.compile("^[A-Z]$");

    private final List<Agent> instantiatedAgents = new ArrayList<>();

    public GenericConfig() {
        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();
        topicManager.clear();
    }

    public void setConfFile(String configPath) {
        this.configPath = configPath;
    }

    public void create() {
        if (configPath == null) return;

        List<String> lines = getText();

        if (lines.size() % 3 != 0) return;

        for (int i = 0; i < lines.size(); i += 3) {
            String agentType = lines.get(i);
            String[] subs = parseTopics(lines.get(i + 1));
            String[] pubs = parseTopics(lines.get(i + 2));

            try {
                Class<?> agentClass = Class.forName(agentType);
                Constructor<?> constructor = agentClass.getConstructor(String[].class, String[].class);
                Agent agent = (Agent) constructor.newInstance((Object)subs, (Object)pubs);
                ParallelAgent parallelAgent = new ParallelAgent(agent, 10);
                instantiatedAgents.add(parallelAgent);

            } catch (Exception e) {
                System.err.println("Could not load agent: " + agentType);
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

    private String[] parseTopics(String line) {
        return line.split(",");
    }

    //TODO these functions are not used
    private void validateTopicCount(String line, int expectedCount, int lineNumber, String agentType, String action) {
        String[] topics = parseTopics(line);
        if (topics.length != expectedCount) {
            throw new RuntimeException(agentType + " at line " + lineNumber + " must have exactly " + expectedCount + " " + action + " topic(s). Found: " + topics.length);
        }
    }

    private void validateTopicLine(String line, String lineType, int lineNumber) {
        if (line.isEmpty()) {
            throw new RuntimeException(lineType + " line " + lineNumber + " is empty.");
        }

        if (!line.equals(line.trim())) {
            throw new RuntimeException(lineType + " line " + lineNumber + " has leading or trailing spaces: '" + line + "'");
        }

        if (line.endsWith(",")) {
            throw new RuntimeException(lineType + " line " + lineNumber + " ends with a comma: '" + line + "'");
        }

        String[] topics = parseTopics(line);

        for (String topic : topics) {
            if (!TOPIC_PATTERN.matcher(topic).matches()) {
                throw new RuntimeException(lineType + " line " + lineNumber + " contains invalid topic format: '" + topic + "'. Topic must be exactly one uppercase letter (A-Z).");
            }
            if (topic.contains(" ")) {
                throw new RuntimeException(lineType + " line " + lineNumber + " contains spaces in topic: '" + topic + "'");
            }
            if (topic.isEmpty()) {
                throw new RuntimeException(lineType + " line " + lineNumber + " contains empty topic in: '" + line + "'");
            }
        }
    }

    private void validateAgentType(String agentType, int lineNumber) {
        if (!agentType.equals(agentType.trim())) {
            throw new RuntimeException("Agent type line " + lineNumber + " has leading or trailing spaces: '" + agentType + "'");
        }

        try {
            Class<?> agentClass = Class.forName(agentType);

            if (!Agent.class.isAssignableFrom(agentClass)) {
                throw new RuntimeException("Class " + agentType + " at line " + lineNumber + " does not implement Agent interface.");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Agent class not found at line " + lineNumber + ": " + agentType);
        }
    }

    private List<String> getText() {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            //TODO handle this
        }

        return lines;
    }

    public void close() {
        for (Agent agent : instantiatedAgents) {
            agent.close();
        }
        instantiatedAgents.clear();
    }
}