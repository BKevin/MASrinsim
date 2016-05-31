package main.route.evaluation;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import main.cbba.agent.AbstractConsensusAgent;

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
//        this.route = v.getRoute();
//
//        this.startTime = startTime;
//        this.startPosition = startPosition;
//
//        this.pickupTimes = new HashMap<>();
//        this.deliveryTimes = new HashMap<>();
//
//        computeRouteTimes(v, timeUnit);
//    }

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

    protected void computeRouteTimes(PDPModel pdpModel, AbstractConsensusAgent v, Unit<Duration> timeUnit){

        final Map<Parcel, Long> pickupTimes = new HashMap();
        final Map<Parcel, Long> deliveryTimes = new HashMap();

        Point currentPosition = this.startPosition;
        long currentTime = this.startTime;


        for(Parcel p : this.getPath()){
            long pickupTime;
            long deliveryTime;

            if (pdpModel.getParcelState(p) == PDPModel.ParcelState.AVAILABLE
                    || pdpModel.getParcelState(p) == PDPModel.ParcelState.ANNOUNCED){ //FIXME niet zeker van ANNOUNCED

                //From last parcel delivery to current parcel pickup
                long pickupTravelTime = v.computeTravelTimeFromTo(currentPosition, p.getPickupLocation(), timeUnit);
                // Assuming TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED
                pickupTime = currentTime
                        + (p.canBePickedUp(v, pickupTravelTime) ? pickupTravelTime : p.getPickupTimeWindow().begin());
                pickupTimes.put(p, pickupTime);

            }
            else{
                pickupTime = currentTime;
            }

            if(pdpModel.getParcelState(p) != PDPModel.ParcelState.DELIVERED
                    && pdpModel.getParcelState(p) != PDPModel.ParcelState.DELIVERING) {

                //From current parcel pickup to current parcel delivery
                long deliveryTravelTime = v.computeTravelTimeFromTo(p.getPickupLocation(), p.getDeliveryLocation(), timeUnit);
                // Assuming TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED
                deliveryTime = pickupTime
                        + p.getPickupDuration()
                        + (p.canBeDelivered(v, deliveryTravelTime) ? deliveryTravelTime : p.getDeliveryTimeWindow().begin());
                deliveryTimes.put(p, deliveryTime);


                // Update current position (only if you had to move to deliver it)
                currentPosition = p.getDeliveryLocation();
                // Update current time (only if you had to move to deliver it)
                currentTime = deliveryTime;
            }


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

}
