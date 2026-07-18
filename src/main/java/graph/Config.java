package graph;

/**
 * Builds the agents that make up the computational graph.
 * {@link GenericConfig} is the implementation we use.
 */
public interface Config {
    /** Builds the agents and wires them to their topics. */
    void create();

    /** @return the configuration's name */
    String getName();

    /** @return the configuration's version number */
    int getVersion();

    /** Tears down the agents created by {@link #create()}, releasing their resources. */
    void close();
}
