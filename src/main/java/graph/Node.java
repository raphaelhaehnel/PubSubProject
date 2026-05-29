package graph;

import java.util.ArrayList;
import java.util.List;


/**
 * One element in the graph view. A node represents either a Topic
 * (name prefixed with "T") or an Agent (name prefixed with "A").
 * Edges are directed.
 */
public class Node {
    private String name;
    private List<Node> edges;
    private Message msg;

    public Node(String name) {
        this.name = name;
        this.edges = new ArrayList<>();
    }

    public void addEdge(Node node) {
        if (!edges.contains(node)) {
            edges.add(node);
        }
    }

    public boolean hasCycles() {
        return hasCyclesHelper(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * DFS-based cycle detection. {@code visited} stores every node ever
     * explored; {@code stack} stores the nodes on the current recursion
     * path. A cycle exists iff we reach a node already on the stack.
     */
    private boolean hasCyclesHelper(List<Node> visited, List<Node> stack) {
        if (!visited.contains(this)) {
            visited.add(this);
            stack.add(this);

            for (Node neighbor : edges) {
                if (!visited.contains(neighbor)) {
                    if (neighbor.hasCyclesHelper(visited, stack)) {
                        return true;
                    }
                } else if (stack.contains(neighbor)) {
                    return true;
                }
            }
        }

        stack.remove(this);
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Node> getEdges() {
        return edges;
    }

    public void setEdges(List<Node> edges) {
        this.edges = edges;
    }

    public Message getMsg() {
        return msg;
    }

    public void setMsg(Message msg) {
        this.msg = msg;
    }
}
