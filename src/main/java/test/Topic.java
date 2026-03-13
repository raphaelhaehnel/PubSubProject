package test;

import java.util.ArrayList;
import java.util.List;

class Topic {
    public final String name;
    public List<Agent> subs;
    public List<Agent> pubs;

    Topic(String name) {
        this.name = name;
        this.subs = new ArrayList<>();
        this.pubs = new ArrayList<>();
    }

    public void subscribe(Agent agent) {
        if (!subs.contains(agent)) {
            subs.add(agent);
        }
    }

    public void unsubscribe(Agent agent) {
        subs.remove(agent);
    }

    public void publish(Message message) {
        pubs.forEach(publisher -> publisher.callback(this.name, message));
    }

    public void addPublisher(Agent agent) {
        if (!pubs.contains(agent)) {
            pubs.add(agent);
        }
    }

    public void removePublisher(Agent agent) {
        pubs.remove(agent);
    }
}
