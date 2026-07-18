package graph.agents;

/**
 * Publishes the sum of all input topics on pubs[0].
 */
public class PlusAgent extends AggregatorAgent {

    /**
     * @param subs the input topic names
     * @param pubs the output topic names; the sum is published on {@code pubs[0]}
     */
    public PlusAgent(String[] subs, String[] pubs) {
        super(subs, pubs);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the sum of all inputs.
     */
    @Override
    protected double aggregate(Double[] values) {
        double sum = 0.0;
        for (Double d : values) {
            sum += d;
        }
        return sum;
    }
}
