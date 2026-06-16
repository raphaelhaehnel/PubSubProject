package graph;

import graph.agents.IncAgent;
import graph.agents.PlusAgent;
import graph.agents.ParallelAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiAgentIntegrationTest {

    @BeforeEach
    void setUp() {
        TopicManagerSingleton.get().clear();
    }

    @Test
    void testFullGraphPipeline() throws InterruptedException {
        // Pipeline: (A + B) -> PlusAgent -> C -> IncAgent -> D
        // Logic: (5 + 10) + 1 = 16
        PlusAgent plus = new PlusAgent(new String[]{"A", "B"}, new String[]{"C"});
        IncAgent inc = new IncAgent(new String[]{"C"}, new String[]{"D"});
        
        // Wrap in ParallelAgent to ensure async propagation
        ParallelAgent pPlus = new ParallelAgent(plus, 5);
        ParallelAgent pInc = new ParallelAgent(inc, 5);

        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic("A").publish(new Message(5.0));
        tm.getTopic("B").publish(new Message(10.0));

        // Give the background threads a moment to propagate the messages
        Thread.sleep(200); 

        Topic outputTopic = tm.getTopic("D");
        assertNotNull(outputTopic.getLastMessage(), "Pipeline D should have received a message.");
        assertEquals(16.0, outputTopic.getLastMessage().asDouble, "Pipeline failed calculation.");
        
        pPlus.close();
        pInc.close();
    }
}