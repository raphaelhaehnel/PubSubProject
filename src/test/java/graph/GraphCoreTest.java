package graph;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GraphCoreTest {

    @Test
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
    void testNodeDetectsNoCycleInAcyclicGraph() {
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");
        Node nodeC = new Node("C");

        nodeA.addEdge(nodeB);
        nodeB.addEdge(nodeC);

        assertFalse(nodeA.hasCycles(), "Graph is a straight line; should not detect cycles.");
    }

    @Test
    void testNodeDetectsSimpleCycle() {
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");

        nodeA.addEdge(nodeB);
        nodeB.addEdge(nodeA);

        assertTrue(nodeA.hasCycles(), "Graph has a simple circular loop; must detect cycle.");
    }

    @Test
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
    void testNodeDetectsSelfLoop() {
        Node nodeA = new Node("A");
        nodeA.addEdge(nodeA);
        assertTrue(nodeA.hasCycles(), "Graph has a self-loop; must detect cycle.");
    }

    @Test
    void testTopicPubSubFlow() {
        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();
        topicManager.clear();

        Topic topic = topicManager.getTopic("TestTopic");
        Message msg = new Message(100.0);
        
        topic.publish(msg);
        
        assertEquals(msg, topic.getLastMessage(), "Topic should store the last published message.");
    }

    @Test
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