package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps an agent so its callback runs on its own thread.
 * The publishing thread just drops the message in a bounded queue and
 * returns immediately, so a slow agent cannot block its publishers.
 */
public class ParallelAgent implements Agent {

    private final BlockingQueue<Message> messagesQueue;
    private final Thread thread;
    private final Agent agent;
    private String currentTopic;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public ParallelAgent(Agent agent, int capacity) {
        this.agent = agent;
        this.messagesQueue = new ArrayBlockingQueue<>(capacity);
        this.thread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    Message message = messagesQueue.take();
                    agent.callback(currentTopic, message);
                } catch (InterruptedException e) {
                    // Nothing to do here
                }
            }
        });
        thread.start();
    }

    @Override
    public String getName() {
        return agent.getName();
    }

    @Override
    public void callback(String topic, Message msg) {
        try {
            currentTopic = topic;
            messagesQueue.put(msg);
        } catch (InterruptedException e) {
            // ignored: caller will get back control
        }
    }

    @Override
    public void reset() {
        agent.reset();
    }

    @Override
    public void close() {
        isRunning.set(false);
        thread.interrupt();
        agent.close();
    }
}
