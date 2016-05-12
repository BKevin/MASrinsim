package main.route;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import main.MyVehicle;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;
import java.util.*;

/**
 * Created by pieter on 12.05.16.
 */
public class RouteTimes{

    private final Collection<Parcel> route;

    // Start time
    private final Long startTime;
    // Start position
    private final Point startPosition;

    // Pickup times for every parcel int he route
    private final Map<Parcel, Long> pickupTimes;
    // Delivery times for every parcel in the route
    private final Map<Parcel, Long> deliveryTimes;


    public RouteTimes(RouteFollowingVehicle v, Point startPosition, Long startTime, Unit<Duration> timeUnit) {
        this.route = v.getRoute();

        this.startTime = startTime;
        this.startPosition = startPosition;

        this.pickupTimes = new HashMap<>();
        this.deliveryTimes = new HashMap<>();

        computeRouteTimes(v, timeUnit);
    }


    public Collection<Parcel> getRoute() {
        return route;
    }

    public Map<Parcel, Long> getPickupTimes() {
        return pickupTimes;
    }

    public Map<Parcel, Long> getDeliveryTimes() {
        return deliveryTimes;
    }

    protected void computeRouteTimes(Vehicle v, Unit<Duration> timeUnit){

        final Map<Parcel, Long> pickupTimes = new HashMap<>();
        final Map<Parcel, Long> deliveryTimes = new HashMap<>();

        Point currentPosition = this.startPosition;

        for(Parcel p : this.getRoute()){

            //From last parcel delivery to current parcel pickup
            //FIXME not linked yet
            long pickupTravelTime = 0;//v.computeTravelTimeFromTo(currentPosition, p.getPickupLocation(), timeUnit);
            //From current parcel pickup to current parcel delivery
            //FIXME not linked yet
            long deliveryTravelTime = 0;//v.computeTravelTimeFromTo(p.getPickupLocation(), p.getDeliveryLocation(), timeUnit);

            // Update current position
            currentPosition = p.getDeliveryLocation();

            // Assuming TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED
            long pickupTime = this.startTime
                    + (p.canBePickedUp(v, pickupTravelTime) ? pickupTravelTime : p.getPickupTimeWindow().begin());
            long deliveryTime = pickupTime
                    + p.getPickupDuration()
                    + (p.canBeDelivered(v, deliveryTravelTime) ? deliveryTravelTime : p.getDeliveryTimeWindow().begin());

            pickupTimes.put(p, pickupTime);
            deliveryTimes.put(p, deliveryTime);
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

        return this.getRoute().equals(rt.getRoute())
                && this.getDeliveryTimes().equals(rt.getDeliveryTimes())
                && this.getPickupTimes().equals(rt.getPickupTimes());
    }

}
