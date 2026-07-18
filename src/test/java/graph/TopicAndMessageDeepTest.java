package graph;

import graph.agents.Agent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Deeper unit tests for {@link Topic} publish delivery (using a Mockito-mocked {@link Agent})
 * and {@link Message} construction from raw bytes.
 */
@DisplayName("Topic delivery and Message construction")
class TopicAndMessageDeepTest {

    @Test
    @DisplayName("Every publish delivers a callback to subscribers")
    void testTopicPubSubThreadSafety() {
        Topic topic = new Topic("SafeTopic");
        Agent mockAgent = Mockito.mock(Agent.class);
        
        topic.subscribe(mockAgent);
        
        // Ensure multiple publishes don't throw exceptions
        assertDoesNotThrow(() -> {
            for(int i=0; i<100; i++) topic.publish(new Message(i * 1.0));
        });
        
        verify(mockAgent, times(100)).callback(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Message built from bytes exposes text, double, and raw data")
    void testMessageBinaryConstructor() {
        String original = "123.0";
        Message msg = new Message(original.getBytes(StandardCharsets.UTF_8));
        
        assertEquals(original, msg.asText);
        assertEquals(123.0, msg.asDouble, 0.001);
        assertArrayEquals(original.getBytes(StandardCharsets.UTF_8), msg.data);
    }
}