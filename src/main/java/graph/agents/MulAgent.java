package graph.agents;

/**
 * Publishes the product of all input topics on pubs[0].
 */
public class MulAgent extends AggregatorAgent {

    /**
     * @param subs the input topic names
     * @param pubs the output topic names; the product is published on {@code pubs[0]}
     */
    public MulAgent(String[] subs, String[] pubs) {
        super(subs, pubs);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the product of all inputs.
     */
    @Override
    protected double aggregate(Double[] values) {
        double product = 1.0;
        for (Double d : values) {
            product *= d;
        }
        return product;
    }
}
