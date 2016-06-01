package mas.route.evaluation.strategy;

import mas.route.evaluation.RouteTimes;

/**
 * Strategy pattern for RouteTimes valuation
 */
public interface RouteValueStrategy {

    Long computeValue(RouteTimes times);
}
