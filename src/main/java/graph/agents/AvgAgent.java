package graph.agents;

/**
 * Publishes the average of all input topics on pubs[0].
 */
public class AvgAgent extends AggregatorAgent {

    public AvgAgent(String[] subs, String[] pubs) {
        super(subs, pubs);
    }

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
