package main.route.evaluation;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import main.cbba.agent.AbstractConsensusAgent;
import main.route.evaluation.strategy.RouteValueStrategy;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;
import java.util.*;

/**
 * Created by pieter on 12.05.16.
 */
public class RouteTimes{

    private final Collection<? extends Parcel> path;

    // Start time
    private final Long startTime;

    // Start position
    private final Point startPosition;
    // Pickup times for every parcel int he route
    private final Map<Parcel, Long> pickupTimes;

    // Delivery times for every parcel in the route
    private final Map<Parcel, Long> deliveryTimes;
//    public RouteTimes(RouteFollowingVehicle v, Point startPosition, Long startTime, Unit<Duration> timeUnit) {


    public RouteTimes(PDPModel pdpModel, AbstractConsensusAgent v, Collection<? extends Parcel> path, Point startPosition, Long startTime, Unit<Duration> timeUnit){
        this.path = path;
        this.startTime = startTime;
        this.startPosition = startPosition;

        this.pickupTimes = new HashMap<>();
        this.deliveryTimes = new HashMap<>();

        computeRouteTimes(pdpModel, v, timeUnit);
    }

    public Collection<? extends Parcel> getPath() {
        return path;
    }


    public Map<? extends Parcel, Long> getPickupTimes() {
        return pickupTimes;
    }

    public Map<? extends Parcel, Long> getDeliveryTimes() {
        return deliveryTimes;
    }

    /**
     * Compute the predicted pickupTimes and deliveryTimes of the parcels on the path.
     * The times are negative (-1) if they already happened in the past.
     * @param pdpModel
     * @param v
     * @param timeUnit
     */
    protected void computeRouteTimes(PDPModel pdpModel, AbstractConsensusAgent v, Unit<Duration> timeUnit){

        final Map<Parcel, Long> pickupTimes = new HashMap();
        final Map<Parcel, Long> deliveryTimes = new HashMap();

        Point currentPosition = this.startPosition;
        long currentTime = this.startTime;


        for(Parcel p : this.getPath()) {
            long pickupTime;
            long deliveryTime;
            //you start at the deliverypoint or underway to the first in path

            //calculate the pickupTime
            long pickupTravelTime = v.computeTravelTimeFromTo(currentPosition, p.getPickupLocation(), timeUnit);
            pickupTime = currentTime
                    + (!p.getPickupTimeWindow().isBeforeStart(currentTime+pickupTravelTime) ? pickupTravelTime : p.getPickupTimeWindow().begin())
                    + p.getPickupDuration();
            pickupTimes.put(p, pickupTime);

            //set the time and position correct
            currentTime = pickupTime;
            currentPosition = p.getPickupLocation();

            //calculate the deliveryTime
            long deliveryTravelTime = v.computeTravelTimeFromTo(currentPosition, p.getDeliveryLocation(), timeUnit);
            deliveryTime = currentTime
                    + (!p.getDeliveryTimeWindow().isBeforeStart(currentTime+deliveryTravelTime) ? deliveryTravelTime : p.getDeliveryTimeWindow().begin())
                    + p.getDeliveryDuration();
            deliveryTimes.put(p, deliveryTime);

            //set the time and position correct
            currentTime = deliveryTime;
            currentPosition = p.getDeliveryLocation();
        }

        this.pickupTimes.putAll(pickupTimes);
        this.deliveryTimes.putAll(deliveryTimes);
    }

    @Override
    public boolean equals(Object o) {

        if(!(o instanceof RouteTimes)){
            return false;
        }

        RouteTimes rt = (RouteTimes) o;

        return this.getPath().equals(rt.getPath())
                && this.getDeliveryTimes().equals(rt.getDeliveryTimes())
                && this.getPickupTimes().equals(rt.getPickupTimes());
    }


    //    }
//        computeRouteTimes(v, timeUnit);
//
//        this.deliveryTimes = new HashMap<>();
//        this.pickupTimes = new HashMap<>();
//
//        this.startPosition = startPosition;
//        this.startTime = startTime;
//
//        this.route = v.getRoute();

    public Point getStartPosition() {
        return startPosition;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getValue(RouteValueStrategy strategy){
        return strategy.computeValue(this);
    }

}
