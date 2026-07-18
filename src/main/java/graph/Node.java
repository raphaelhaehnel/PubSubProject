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

    /**
     * Creates a node with no edges.
     *
     * @param name the node name (topics are prefixed with {@code "T"}, agents with {@code "A"})
     */
    public Node(String name) {
        this.name = name;
        this.edges = new ArrayList<>();
    }

    /**
     * Adds a directed edge from this node to {@code node}, ignoring duplicates.
     *
     * @param node the destination node
     */
    public void addEdge(Node node) {
        if (!edges.contains(node)) {
            edges.add(node);
        }
    }

    /**
     * Checks whether this node lies on a directed cycle.
     *
     * @return {@code true} if a cycle is reachable from this node, {@code false} otherwise
     */
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

    /** @return the node's name */
    public String getName() {
        return name;
    }

    /** @param name the new node name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the outgoing edges of this node */
    public List<Node> getEdges() {
        return edges;
    }

    /** @param edges the new list of outgoing edges */
    public void setEdges(List<Node> edges) {
        this.edges = edges;
    }

    /** @return the latest message associated with this node, or {@code null} if none */
    public Message getMsg() {
        return msg;
    }

    /** @param msg the latest message to associate with this node */
    public void setMsg(Message msg) {
        this.msg = msg;
    }
}
