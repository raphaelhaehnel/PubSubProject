package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
//                    System.out.println("ParallelAgent thread interrupted");
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
//            System.out.println("Agent callback interrupted");
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
