package graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pub-sub core: {@link Message} conversions, {@link Node} cycle detection,
 * and basic {@link Topic} publish/subscribe behavior.
 */
@DisplayName("Graph core (Message, Node, Topic)")
class GraphCoreTest {

    @Test
    @DisplayName("Message stores payload as text, number, and NaN for non-numbers")
    void testMessageConversions() {
        Message textMsg = new Message("HelloWorld");
        assertEquals("HelloWorld", textMsg.asText);
        assertTrue(Double.isNaN(textMsg.asDouble), "Non-numeric text should yield Double.NaN");

        Message strNumMsg = new Message("3.1415");
        assertEquals(3.1415, strNumMsg.asDouble, "String should be correctly parsed to Double");

        Message doubleMsg = new Message(42.0);
        assertEquals("42.0", doubleMsg.asText);
        assertEquals(42.0, doubleMsg.asDouble);
    }

    @Test
    @DisplayName("A linear chain of nodes has no cycle")
    void testNodeDetectsNoCycleInAcyclicGraph() {
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");
        Node nodeC = new Node("C");

        nodeA.addEdge(nodeB);
        nodeB.addEdge(nodeC);

        assertFalse(nodeA.hasCycles(), "Graph is a straight line; should not detect cycles.");
    }

    @Test
    @DisplayName("A two-node loop is detected as a cycle")
    void testNodeDetectsSimpleCycle() {
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");

        nodeA.addEdge(nodeB);
        nodeB.addEdge(nodeA);

        assertTrue(nodeA.hasCycles(), "Graph has a simple circular loop; must detect cycle.");
    }

    @Test
    @DisplayName("A cycle nested deeper in the graph is detected")
    void testNodeDetectsComplexCycle() {
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");
        Node nodeC = new Node("C");
        Node nodeD = new Node("D");

        nodeA.addEdge(nodeB);
        nodeB.addEdge(nodeC);
        nodeC.addEdge(nodeD);
        nodeD.addEdge(nodeB); 

        assertTrue(nodeA.hasCycles(), "Graph has a deep cycle; must detect cycle.");
    }

    @Test
    @DisplayName("A self-loop is detected as a cycle")
    void testNodeDetectsSelfLoop() {
        Node nodeA = new Node("A");
        nodeA.addEdge(nodeA);
        assertTrue(nodeA.hasCycles(), "Graph has a self-loop; must detect cycle.");
    }

    @Test
    @DisplayName("Topic remembers the last published message")
    void testTopicPubSubFlow() {
        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();
        topicManager.clear();

        Topic topic = topicManager.getTopic("TestTopic");
        Message msg = new Message(100.0);
        
        topic.publish(msg);
        
        assertEquals(msg, topic.getLastMessage(), "Topic should store the last published message.");
    }

    @Test
    @DisplayName("A cycle is detected even with a dead-end branch present")
    void testNodeDetectsCycleWithSinkBranch() {
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");
        Node nodeC = new Node("C");
        Node nodeSink = new Node("Sink");

        // A -> B -> C -> A (The Cycle)
        // C -> Sink (The Dead-End Branch)
        nodeA.addEdge(nodeB);
        nodeB.addEdge(nodeC);
        nodeC.addEdge(nodeA); 
        nodeC.addEdge(nodeSink); 

        assertTrue(nodeA.hasCycles(), "Graph must detect the cycle even if a branch leads to a dead-end sink.");
    }
}