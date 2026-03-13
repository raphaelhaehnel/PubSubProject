package graph;

import java.util.List;

public class Topic {
    public final String name;
    public List<Agent> subsList;
    public List<Agent> pubsList;

    Topic() {

    }

    public void subscribe(Agent agent) {
    }

    public void unsubscribe(Agent agent) {
    }

    public void publish(Message message) {
        pubsList.forEach(publisher -> publisher.callback(this.name, message));
    }

    public void addPublisher(Agent agent) {
        pubsList.add(agent);
    }

    public void removePublisher(Agent agent) {
        pubsList.remove(agent);
    }
}
