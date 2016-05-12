package main.route;

import main.MyParcel;

import java.util.*;

/**
 * Created by pieter on 12.05.16.
 */
public class RouteEvaluation {

    private final RouteTimes times;

    private Long penalty;

    RouteEvaluation(RouteTimes routeTimes){
        this.times = routeTimes;
        this.computeRouteValue();
    }

//    private Collection<Parcel> computeOptimalRoute(Collection<Parcel> route){

//
//        }
//            computeRouteValue(route)
//        for(Collection<Parcel> route : routes){
//
//        List<Collection<Parcel>> routes = this.generateRoutes(this.getRoute());
//

    public Long getPenalty() {
        return penalty;
    }

    public void setPenalty(Long penalty) {
        this.penalty = penalty;
    }

    public RouteTimes getTimes() {
        return times;
    }

    /**
     * Computes route value based on each individual parcel's penalty incurred on pickup and delivery.
     */
    private long computeRouteValue() {

        long penalty = 0L;

        for(MyParcel p : Arrays.asList(this.times.getRoute().toArray(new MyParcel[0]))) {
            penalty += p.computePenalty(this.times.getPickupTimes().get(p), this.times.getDeliveryTimes().get(p));
        }

        return penalty;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof RouteEvaluation)){
            return false;
        }

        RouteEvaluation re = (RouteEvaluation) o;

        return re.getPenalty().equals(this.getPenalty()) && re.getTimes().equals(this.getTimes());
    }
}
