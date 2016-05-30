package main.route.evaluation;

import com.github.rinde.rinsim.core.model.pdp.Parcel;

import java.util.*;

/**
 * Created by pieter on 12.05.16.
 */
public class RouteEvaluation {

    private final RouteTimes times;

    private final Penalty penalty;

    public RouteEvaluation(RouteTimes routeTimes){
        this.times = routeTimes;
        this.penalty = this.computeRoutePenalty();
    }

    public Penalty getPenalty() {
        return penalty;
    }

    public RouteTimes getTimes() {
        return times;
    }

    /**
     * Computes route value based on each individual parcel's penalty incurred on pickup and delivery.
     */
    protected Penalty computeRoutePenalty() {

        Map<? extends Parcel, Long> map = new HashMap<>();

        for(Parcel p : Arrays.asList(this.times.getRoute().toArray(new Parcel[0]))) {
            map.put(p, p.computePenalty(this.times.getPickupTimes().get(p), this.times.getDeliveryTimes().get(p)));
        }

        return new Penalty(this.times.getRoute(), map);
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
