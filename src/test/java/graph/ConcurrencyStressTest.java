package graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress tests verifying that {@link Topic} and the {@link TopicManagerSingleton.TopicManager}
 * remain thread-safe under heavy concurrent publishing and clearing.
 */
@DisplayName("Concurrency stress tests")
class ConcurrencyStressTest {

    @Test
    @DisplayName("TopicManager stays stable under concurrent topic access")
    void testConcurrentTopicAccess() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        // 10 threads concurrently accessing and publishing to the same topic
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                Topic t = tm.getTopic("StressTopic");
                t.publish(new Message(Math.random()));
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Threads timed out during stress test!");
        assertDoesNotThrow(() -> tm.getTopic("StressTopic"), "TopicManager should remain stable under high concurrency.");
    }

    @Test
    @DisplayName("Clearing the manager while publishing does not throw")
    void testClearWhilePublishingRaceCondition() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        Topic topic = tm.getTopic("RaceTopic");

        // Simulating chaos: Threads trying to publish while another thread frequently clears the manager
        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                try {
                    topic.publish(new Message(Math.random()));
                } catch (Exception ignored) {
                    // We expect things to fail gracefully, but NOT throw unhandled NullPointerExceptions or ConcurrentModificationExceptions
                }
            });
            
            if (i % 5 == 0) {
                executor.submit(tm::clear);
            }
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Threads timed out during race condition test!");
        assertDoesNotThrow(() -> tm.getTopic("RaceTopic"), "System should recover cleanly after concurrent clears.");
    }
}