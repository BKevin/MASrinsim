package main;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;
import comm.AcceptBidMessage;
import comm.AuctionedParcelMessage;
import comm.BidMessage;
import comm.RefuseBidMessage;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by KevinB on 8/05/2016.
 */
public class MyVehicle extends RouteFollowingVehicle implements CommUser{


    private Optional<CommDevice> device;

    // Index on which a Parcel would be added if the bid for that Parcel is won.
    private Map<Parcel, Integer> calculatedIndexOfParcel;

    public MyVehicle(VehicleDTO vehicleDTO) {
        super(vehicleDTO, true); //route diversion is on.

        calculatedIndexOfParcel = new HashMap<>();
    }


    /**
     * Pre-tick message handling.
     *
     * May alter route when parcel is won.
     *
     * @param time
     */
    @Override
    protected void preTick(TimeLapse time) {
        super.preTick(time);

        communicate();

        time.getTimeUnit();
    }

    private void communicate() {
        if(device.get().getUnreadCount() == 0)
            return;
        //check new messages
        boolean haveToRecalculate = false;

        for(Message message : device.get().getUnreadMessages()){
            //if AuctionedParcelMessage then calculate bid and send BidMessage
            final MessageContents contents = message.getContents();

            if(contents instanceof AuctionedParcelMessage){
                AuctionedParcelMessage auctionedParcelMessage = (AuctionedParcelMessage) contents;
                //TODO change to asynchonous call(?)
                int bidInfo = calculateBidInfo(auctionedParcelMessage);
                device.get().send(new BidMessage(bidInfo, this), message.getSender());
            }
            //if AcceptBidMessage then add parcel to todolist
            if(contents instanceof AcceptBidMessage){
                // The Vehicle won the auction.
                AcceptBidMessage acceptedBidMessage = (AcceptBidMessage) contents;

                // Add vehicle to route
                LinkedList newRoute = new LinkedList<>(this.getRoute());

                newRoute.add(
                        calculatedIndexOfParcel.get(acceptedBidMessage.getParcel()),
                        acceptedBidMessage.getParcel());

                this.setRoute(newRoute);

            }

            if(contents instanceof RefuseBidMessage){

                RefuseBidMessage refuseBidMessage = (RefuseBidMessage) contents;

                calculatedIndexOfParcel.remove(refuseBidMessage.getParcel());

            }

            //if RefuseBidMessage then remove from memory (incase of reauctioned)
            //else not applicable

        }

        if(haveToRecalculate){
            //recalculate utility for situation
//            calculateWithIDP();
            //request new auction for parcels incurring a penalty
            //TODO penalty + when to reauction
        }


    }

    private int calculateBidInfo(AuctionedParcelMessage contents) {






        //calculate Distances between destinatinons
        return 0;
    }

    private long computeRouteLength(Iterable<Parcel> route){

        Point currentPosition = this.getRoadModel().getPosition(this);
        Unit<Duration> unit = this.getCurrentTimeLapse().getTimeUnit();

        final Map<Parcel, Long> pickupTimes = new HashMap<>();
        final Map<Parcel, Long> deliveryTimes = new HashMap<>();

        for(Parcel p : this.getRoute()){

            //From last parcel delivery to current parcel pickup
            long pickupTravelTime = this.computeTravelTimeFromTo(currentPosition, p.getPickupLocation(), unit);
            //From current parcel pickup to current parcel delivery
            long deliveryTravelTime = this.computeTravelTimeFromTo(p.getPickupLocation(), p.getDeliveryLocation(), unit);

            // Update current position
            currentPosition = p.getDeliveryLocation();

            // Assuming TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED
            long pickupTime = getCurrentTime()
                    + (p.canBePickedUp(this, pickupTravelTime) ? pickupTravelTime : p.getPickupTimeWindow().begin());
            long deliveryTime = pickupTime
                    + p.getPickupDuration()
                    + (p.canBeDelivered(this, deliveryTravelTime) ? deliveryTravelTime : p.getDeliveryTimeWindow().begin());

            pickupTimes.put(p, pickupTime);
            deliveryTimes.put(p, deliveryTime);
        }

        // Possibly unsafe cast: Assuming Collection<Parcel> is Collection<MyParcel>
        computeRoutePenalty(pickupTimes, deliveryTimes, Arrays.asList(this.getRoute().toArray(new MyParcel[0])));

        return 0L;
    }

    private long computeRoutePenalty(Map<Parcel, Long> pickupTimes, Map<Parcel, Long> deliveryTimes, Collection<MyParcel> route) {
        // TODO add caching to route penalty calculations

        long penalty = 0L;

        for(MyParcel p : route) {
            penalty += p.computePenalty(pickupTimes.get(p), deliveryTimes.get(p));
        }

        return penalty;
    }


    @Override
    protected long computeTravelTimeTo(Point p, Unit<Duration> timeUnit) {

        return this.computeTravelTimeFromTo(this.getRoadModel().getPosition(this), p, timeUnit);
    }

    /**
     * Compute the travel time between two points in the simulation at the speed of this vehicle.
     * @param a
     * @param b
     * @param timeUnit
     * @return
     */
    protected long computeTravelTimeFromTo(Point a, Point b, Unit<Duration> timeUnit) {
        // TODO add caching to travel time calculations

        final Measure<Double, Length> distance = Measure.valueOf(Point.distance(
                a, b), getRoadModel()
                .getDistanceUnit());

        return DoubleMath.roundToLong(
                RoadModels.computeTravelTime(
                        Measure.valueOf(this.getSpeed(), this.getRoadModel().getSpeedUnit()), distance, timeUnit),
                RoundingMode.CEILING);
    }

    @Override
    public Optional<Point> getPosition() {
        if (this.getRoadModel().containsObject(this)) {
            return Optional.of(this.getRoadModel().getPosition(this));
        }
        return Optional.absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        device = Optional.of(commDeviceBuilder.build());
    }



    public void getPenalty(){

    }


}
