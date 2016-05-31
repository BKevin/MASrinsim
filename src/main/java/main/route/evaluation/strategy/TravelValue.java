package main.route.evaluation.strategy;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import main.MyParcel;
import main.route.evaluation.RouteTimes;

import java.util.*;

/**
 * Created by pieter on 01.06.16.
 */
public class TravelValue implements RouteValueStrategy {

    private Map<MyParcel, Long> violationMap;

    @Override
    public Long computeValue(RouteTimes times) {

        return getRouteCost(times);
    }

    public Long getRouteCost(RouteTimes times) {

        Long maxDeliveryTime = times.getDeliveryTimes().entrySet().stream().max(new Comparator<Map.Entry<? extends Parcel, Long>>() {
            @Override
            public int compare(Map.Entry<? extends Parcel, Long> longEntry, Map.Entry<? extends Parcel, Long> t1) {
                return longEntry.getValue().compareTo(t1.getValue());
            }
        }).get().getValue();

        return maxDeliveryTime - times.getStartTime();
    }

    public Long getRoutePenalty(){

        Long penalty = 0L;

        for(Parcel p : this.violationMap.keySet()){
            penalty += violationMap.get(p);
        }

        return penalty;

    }




}
