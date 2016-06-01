package mas.route.evaluation.strategy;

import mas.route.evaluation.RouteTimes;

/**
 * Combination of TravelValue en TimeWindowViolationValue
 */
public class TotalCostValue implements RouteValueStrategy {

    @Override
    public Long computeValue(RouteTimes times) {
        // non-cached implementation
        return new TimeWindowViolationValue().computeValue(times) + new TravelValue().computeValue(times);
    }
}
