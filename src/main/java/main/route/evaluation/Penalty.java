package main.route.evaluation;

import com.github.rinde.rinsim.core.model.pdp.Parcel;

import java.util.Collection;
import java.util.Map;

/**
 * Created by pieter on 16.05.16.
 */
public class Penalty {

    private final Collection<Parcel> route;

    private final Map<Parcel, Long> penaltyMap;

    public Penalty(Collection<Parcel> route, Map<Parcel, Long> penaltyMap) {
        this.route = route;
        this.penaltyMap = penaltyMap;
    }

    public Collection<Parcel> getRoute(){

        return this.route;

    }

    public Long getRoutePenalty(){

        Long penalty = 0L;

        for(Parcel p : this.getRoute()){
            penalty += penaltyMap.get(p);
        }

        return penalty;

    }

    public Long getRoutePenalty(Parcel parcel){

        return this.penaltyMap.get(parcel);

    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Penalty)){
            return false;
        }

        Penalty pe = (Penalty) o;

        return pe.getRoute().equals(this.getRoute()) && pe.getRoutePenalty().equals(this.getRoutePenalty());
    }
}
