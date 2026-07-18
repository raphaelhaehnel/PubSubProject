package graph.agents;

/**
 * Publishes the average of all input topics on pubs[0].
 */
public class AvgAgent extends AggregatorAgent {

    /**
     * @param subs the input topic names
     * @param pubs the output topic names; the average is published on {@code pubs[0]}
     */
    public AvgAgent(String[] subs, String[] pubs) {
        super(subs, pubs);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the arithmetic mean of the inputs, or {@code 0.0} when there are none.
     */
    @Override
    protected double aggregate(Double[] values) {
        if (values.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (Double d : values) {
            sum += d;
        }
        return sum / values.length;
    }
}
