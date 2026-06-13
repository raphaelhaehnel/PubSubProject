package graph.agents;

/**
 * Publishes the product of all input topics on pubs[0].
 */
public class MulAgent extends AggregatorAgent {

    public MulAgent(String[] subs, String[] pubs) {
        super(subs, pubs);
    }

    @Override
    protected double aggregate(Double[] values) {
        double product = 1.0;
        for (Double d : values) {
            product *= d;
        }
        return product;
    }
}
