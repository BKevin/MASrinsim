package main;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;
import main.comm.AcceptBidMessage;
import main.comm.AuctionedParcelMessage;
import main.comm.BidMessage;
import main.comm.RefuseBidMessage;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;
import java.math.RoundingMode;
import java.util.*;

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

    public void communicate() {
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


                //DONT FORGET TO ADD THE PARCEL TWICE, BOTH FOR PICKUP AND DELIVERY
                newRoute.add(
                        //calculatedIndexOfParcel.get(acceptedBidMessage.getParcel()),
                        newRoute.size(), //FIXME just something to make it work (since no implementation of position picking yet)
                        acceptedBidMessage.getParcel());
                newRoute.add(
                        newRoute.size(),
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





    @Override
    protected long computeTravelTimeTo(Point p, Unit<Duration> timeUnit) {

        return this.computeTravelTimeFromTo(this.getRoadModel().getPosition(this), p, timeUnit);
    }

    /**
     * Compute the travel time between two points in the simulation at the speed of this vehicle.
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
