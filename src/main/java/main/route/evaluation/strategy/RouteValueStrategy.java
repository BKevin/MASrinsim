package main.route.evaluation.strategy;

import main.route.evaluation.RouteTimes;

/**
 * Strategy pattern for RouteTimes valuation
 */
public interface RouteValueStrategy {

    Long computeValue(RouteTimes times);
}
