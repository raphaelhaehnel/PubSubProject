package graph.agents;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import graph.Message;

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

    /**
     * Wraps an agent and starts a dedicated worker thread that drains the message queue and
     * delivers each message to the wrapped agent's callback.
     *
     * @param agent    the agent to run on its own thread
     * @param capacity the maximum number of queued messages before {@code callback} blocks
     */
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

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return agent.getName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Enqueues the message for the worker thread and returns immediately, so the publishing
     * thread is never blocked by the wrapped agent's processing.
     */
    @Override
    public void callback(String topic, Message msg) {
        try {
            currentTopic = topic;
            messagesQueue.put(msg);
        } catch (InterruptedException e) {
            // ignored: caller will get back control
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the wrapped agent.
     */
    @Override
    public void reset() {
        agent.reset();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stops the worker thread and closes the wrapped agent.
     */
    @Override
    public void close() {
        isRunning.set(false);
        thread.interrupt();
        agent.close();
    }
}
