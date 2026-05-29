package graph;

/**
 * Builds the agents that make up the computational graph.
 * {@link GenericConfig} is the implementation we use.
 */
public interface Config {
    void create();
    String getName();
    int getVersion();
    void close();
}
