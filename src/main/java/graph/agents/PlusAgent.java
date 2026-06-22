package graph.agents;

/**
 * Publishes the sum of all input topics on pubs[0].
 */
public class PlusAgent extends AggregatorAgent {

    public PlusAgent(String[] subs, String[] pubs) {
        super(subs, pubs);
    }

    @Override
    protected double aggregate(Double[] values) {
        double sum = 0.0;
        for (Double d : values) {
            sum += d;
        }
        return sum;
    }
}
