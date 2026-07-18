package graph.agents;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the mathematical agents: the aggregators ({@link PlusAgent}, {@link MulAgent},
 * {@link AvgAgent}) plus {@link BinOpAgent}, {@link DivAgent}, and {@link IncAgent}, including
 * edge cases such as division by zero, waiting for all inputs, and dropping NaN inputs.
 */
@DisplayName("Math agents")
class MathAgentsTest {

    private TopicManagerSingleton.TopicManager topicManager;

    @BeforeEach
    void setUp() {
        topicManager = TopicManagerSingleton.get();
        topicManager.clear();
    }

    // Provides the data sets for the parameterized test below
    static Stream<Arguments> provideAggregatorData() {
        return Stream.of(
            // PlusAgent tests
            Arguments.of("Plus", new double[]{5.0, 10.0}, 15.0),
            Arguments.of("Plus", new double[]{-5.0, 5.0}, 0.0),
            Arguments.of("Plus", new double[]{0.0, 0.0, 0.0}, 0.0),
            
            // MulAgent tests
            Arguments.of("Mul", new double[]{4.0, 2.5}, 10.0),
            Arguments.of("Mul", new double[]{10.0, 0.0}, 0.0),
            Arguments.of("Mul", new double[]{-2.0, -3.0}, 6.0),
            
            // AvgAgent tests
            Arguments.of("Avg", new double[]{10.0, 20.0, 30.0}, 20.0),
            Arguments.of("Avg", new double[]{5.0}, 5.0),
            Arguments.of("Avg", new double[]{-10.0, 10.0}, 0.0)
        );
    }

    @ParameterizedTest(name = "Testing {0}Agent with expected output {2}")
    @MethodSource("provideAggregatorData")
    @DisplayName("Aggregator agents compute the expected result")
    void testAggregatorAgentsBasicMath(String agentType, double[] inputs, double expected) {
        String[] subs = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            subs[i] = "Topic" + i;
        }
        String[] pubs = new String[]{"Result"};

        // Dynamic instantiation based on the parameterized type
        Agent agent = switch (agentType) {
            case "Plus" -> new PlusAgent(subs, pubs);
            case "Mul" -> new MulAgent(subs, pubs);
            case "Avg" -> new AvgAgent(subs, pubs);
            default -> throw new IllegalArgumentException("Unknown agent type: " + agentType);
        };

        // Publish inputs sequentially
        for (int i = 0; i < inputs.length; i++) {
            topicManager.getTopic(subs[i]).publish(new Message(inputs[i]));
        }

        Topic resultTopic = topicManager.getTopic("Result");
        assertNotNull(resultTopic.getLastMessage(), agentType + "Agent did not publish a result.");
        assertEquals(expected, resultTopic.getLastMessage().asDouble, 0.0001, agentType + "Agent calculation failed.");
    }

    @Test
    @DisplayName("Aggregator waits for every input before its first publish")
    void testAggregatorWaitsForAllInputsBeforePublishing() {
        PlusAgent agent = new PlusAgent(new String[]{"A", "B"}, new String[]{"C"});
        Topic topicA = topicManager.getTopic("A");
        Topic topicB = topicManager.getTopic("B");
        Topic topicC = topicManager.getTopic("C");

        topicA.publish(new Message(5.0));
        
        // It shouldn't publish because topic B hasn't received a value yet
        assertNull(topicC.getLastMessage(), "Aggregator should wait for all input topics.");

        topicB.publish(new Message(10.0));
        assertNotNull(topicC.getLastMessage());
    }

    @Test
    @DisplayName("BinOpAgent applies a custom binary operator")
    void testBinOpAgentWithCustomLambda() {
        // Testing BinOpAgent using Math.max as the binary operator
        BinOpAgent agent = new BinOpAgent("MaxAgent", "A", "B", "Out", Math::max);
        
        topicManager.getTopic("A").publish(new Message(10.0));
        topicManager.getTopic("B").publish(new Message(25.0));

        Topic out = topicManager.getTopic("Out");
        assertNotNull(out.getLastMessage());
        assertEquals(25.0, out.getLastMessage().asDouble, "BinOpAgent failed to apply custom lambda.");
    }

    @Test
    @DisplayName("DivAgent divides numerator by denominator")
    void testDivAgentCalculatesCorrectly() {
        DivAgent agent = new DivAgent(new String[]{"Num", "Den"}, new String[]{"Res"});
        
        topicManager.getTopic("Num").publish(new Message(10.0));
        topicManager.getTopic("Den").publish(new Message(2.0));

        Topic res = topicManager.getTopic("Res");
        assertNotNull(res.getLastMessage());
        assertEquals(5.0, res.getLastMessage().asDouble);
    }

    @Test
    @DisplayName("DivAgent publishes nothing on division by zero")
    void testDivAgentIgnoresDivisionByZero() {
        DivAgent agent = new DivAgent(new String[]{"Num", "Den"}, new String[]{"Res"});
        
        topicManager.getTopic("Num").publish(new Message(10.0));
        topicManager.getTopic("Den").publish(new Message(0.0)); // Trigger edge case

        Topic res = topicManager.getTopic("Res");
        assertNull(res.getLastMessage(), "DivAgent should abort and not publish on division by zero.");
    }

    @Test
    @DisplayName("IncAgent publishes the input plus one")
    void testIncAgentIncrementsValue() {
        IncAgent agent = new IncAgent(new String[]{"Input"}, new String[]{"Output"});
        
        topicManager.getTopic("Input").publish(new Message(7.0));

        Topic output = topicManager.getTopic("Output");
        assertNotNull(output.getLastMessage());
        assertEquals(8.0, output.getLastMessage().asDouble);
    }
    
    @Test
    @DisplayName("Agents drop non-numeric (NaN) messages without publishing")
    void testAgentsIgnoreNaNMessagesAndGracefullyDropThem() {
        IncAgent agent = new IncAgent(new String[]{"Input"}, new String[]{"Output"});
        
        // Sending invalid text to simulate a parsing failure
        topicManager.getTopic("Input").publish(new Message("InvalidTextThatIsNotANumber"));

        Topic output = topicManager.getTopic("Output");
        assertNull(output.getLastMessage(), "Agents must safely ignore Double.NaN inputs.");
    }
}