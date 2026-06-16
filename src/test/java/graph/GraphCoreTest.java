package graph;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GraphCoreTest {

    @Test
    void testMessageConversions() {
        // Test text-only message
        Message textMsg = new Message("HelloWorld");
        assertEquals("HelloWorld", textMsg.asText);
        assertTrue(Double.isNaN(textMsg.asDouble), "Non-numeric text should yield Double.NaN");

        // Test valid numeric string message
        Message strNumMsg = new Message("3.1415");
        assertEquals(3.1415, strNumMsg.asDouble, "String should be correctly parsed to Double");

        // Test direct double message
        Message doubleMsg = new Message(42.0);
        assertEquals("42.0", doubleMsg.asText);
        assertEquals(42.0, doubleMsg.asDouble);
    }

    @Test
    void testNodeDetectsNoCycleInAcyclicGraph() {
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");
        Node nodeC = new Node("C");

        // A -> B -> C
        nodeA.addEdge(nodeB);
        nodeB.addEdge(nodeC);

        assertFalse(nodeA.hasCycles(), "Graph is a straight line; should not detect cycles.");
    }

    @Test
    void testNodeDetectsSimpleCycle() {
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");

        // A -> B -> A
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

        // A -> B -> C -> D -> B (Cycle involves B, C, D)
        nodeA.addEdge(nodeB);
        nodeB.addEdge(nodeC);
        nodeC.addEdge(nodeD);
        nodeD.addEdge(nodeB); 

        assertTrue(nodeA.hasCycles(), "Graph has a deep cycle; must detect cycle.");
    }

    @Test
    void testNodeDetectsSelfLoop() {
        Node nodeA = new Node("A");
        
        // A -> A
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
}