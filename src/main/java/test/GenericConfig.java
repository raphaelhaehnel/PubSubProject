package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class GenericConfig implements Config {

    private String configPath;

    private final List<String> agentTypes = new ArrayList<>();
    private final List<String> subsLines = new ArrayList<>();
    private final List<String> pubsLines = new ArrayList<>();

    private static final Pattern TOPIC_PATTERN = Pattern.compile("^[A-Z]$");

    private static final Set<String> SINGLE_INPUT_AGENTS = Set.of("IncAgent");
    private static final Set<String> DOUBLE_INPUT_AGENTS = Set.of("PlusAgent");

    private final List<Agent> instantiatedAgents = new ArrayList<>();

    public void setConfFile(String configPath) {
        this.configPath = configPath;
    }

    public void create() {
        if (configPath == null) {
            return;
        }
        List<String> lines = getText();

        if (lines.size() % 3 != 0) {
            throw new RuntimeException("Config file format error: total lines is not a multiple of 3");
        }

        for (int i = 0; i < lines.size(); i += 3) {
            agentTypes.add(lines.get(i));
            subsLines.add(lines.get(i + 1));
            pubsLines.add(lines.get(i + 2));
        }

        for (int i = 0; i < agentTypes.size(); i++) {
            String agentType = agentTypes.get(i);
            String subsLine = subsLines.get(i);
            String pubsLine = pubsLines.get(i);

            int agentLineNum = i * 3 + 1;
            int subsLineNum = i * 3 + 2;
            int pubsLineNum = i * 3 + 3;

            validateAgentType(agentType, agentLineNum);
            validateTopicLine(subsLine, "Subscription", subsLineNum);
            validateTopicLine(pubsLine, "Publication", pubsLineNum);

            if (SINGLE_INPUT_AGENTS.contains(agentType)) {
                validateTopicCount(subsLine, 1, subsLineNum, agentType, "listen");
                validateTopicCount(pubsLine, 1, pubsLineNum, agentType, "publish");
            } else if (DOUBLE_INPUT_AGENTS.contains(agentType)) {
                validateTopicCount(subsLine, 2, subsLineNum, agentType, "listen");
                validateTopicCount(pubsLine, 1, pubsLineNum, agentType, "publish");
            }
        }

        Agent agent;
        for (int i = 0; i < agentTypes.size(); i++) {
            String agentType = agentTypes.get(i);
            String subsLine = subsLines.get(i);
            String pubsLine = pubsLines.get(i);
            String[] subs = parseTopics(subsLine);
            String[] pubs = parseTopics(pubsLine);
            try {
                Class<?> agentClass = Class.forName(agentType);
                Constructor<?> constructor = agentClass.getConstructor(String[].class, String[].class);

                agent = (Agent) constructor.newInstance(subs, pubs);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate agent: " + agentType, e);
            }
            ParallelAgent parallelAgent = new ParallelAgent(agent, 10);
            instantiatedAgents.add(parallelAgent);
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