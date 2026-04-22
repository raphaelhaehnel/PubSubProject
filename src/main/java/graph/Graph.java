package graph;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Graph extends ArrayList<Node> {

    public boolean hasCycles() {
        return this.stream().anyMatch(Node::hasCycles);
    }

    public void createFromTopics() {
        clear();
        Map<Topic, Node> topicNodeMap = new ConcurrentHashMap<>();
        Map<Agent, Node> agentNodeMap = new ConcurrentHashMap<>();

        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();

        for (Topic topic : topicManager.getTopics()) {
            Node topicNode = topicNodeMap.computeIfAbsent(topic, t -> {
                Node node = createNewNode("T" + t.name);
                node.setMsg(t.getLastMessage());
                return node;
            });
            connectPublishersToTopic(topic, topicNode, agentNodeMap);
            connectSubscribersToTopic(topic, topicNode, agentNodeMap);
        }
    }

    private Node createNewNode(String name) {
        Node newNode = new Node(name);
        this.add(newNode);
        return newNode;
    }

    private void connectSubscribersToTopic(Topic topic, Node topicNode, Map<Agent, Node> agentNodeMap) {
        for (Agent subscriber : topic.getSubscribers()) {
            Node agentNode = agentNodeMap.computeIfAbsent(subscriber, a -> createNewNode("A" + a.getName()));
            topicNode.addEdge(agentNode);
        }
    }

    private void connectPublishersToTopic(Topic topic, Node topicNode, Map<Agent, Node> agentNodeMap) {
        for (Agent publisher : topic.getPublishers()) {
            Node agentNode = agentNodeMap.computeIfAbsent(publisher, a -> {
                Node node = createNewNode("A" + a.getName());
                node.setMsg(topic.getLastMessage());
                return node;
            });
            agentNode.addEdge(topicNode);
        }
    }

}
