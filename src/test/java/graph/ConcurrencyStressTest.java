package graph;

import org.junit.jupiter.api.Test;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyStressTest {

    @Test
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
}