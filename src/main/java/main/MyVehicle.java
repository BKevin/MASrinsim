package main;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.google.common.base.Optional;
import comm.AcceptBidMessage;
import comm.AuctionedParcelMessage;
import comm.BidMessage;
import comm.RefuseBidMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by KevinB on 8/05/2016.
 */
public class MyVehicle extends RouteFollowingVehicle implements CommUser{


    private Optional<CommDevice> device;

    public MyVehicle(VehicleDTO vehicleDTO) {
        super(vehicleDTO);
    
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
            calculateWithIDP();
            //request new auction for parcels incurring a penalty
            //TODO penalty + when to reauction
        }


    }

    private int calculateBidInfo(AuctionedParcelMessage contents) {
        //calculate Distances between destinatinons
        return 0;
    }


    private void calculateWithIDP() {
        //(calculate Distances between destinatinons)

        //nothing for now
    }


    @Deprecated
    private void move(TimeLapse time) {
        //move in the direction of the first scheduled parcel
        //if arrived then pickup or deliver Parcel
//        final RoadModel rm = getRoadModel();
//        final PDPModel pm = getPDPModel();
//
//        if (!time.hasTimeLeft()) {
//            return;
//        }
//
//        if (parcels.isEmpty()) {
//            //do nothing when no assignment
//            return;
//        }
//
//        if (!parcels.isEmpty()) {
//            MyParcel curr = parcels.get(0);
//            final boolean inCargo = pm.containerContains(this, curr);
//            // sanity check: if it is not in our cargo AND it is also not on the
//            // RoadModel, we cannot go to curr anymore.
//            if (!inCargo && !rm.containsObject(curr)) {
//                throw new IllegalStateException("A Parcel isn't available, but MyVehicle still has to PDP it.");
//            } else if (inCargo) {
//                // if it is in cargo, go to its destination
//                rm.moveTo(this, curr.getDeliveryLocation(), time);
//                if (rm.getPosition(this).equals(curr.getDeliveryLocation())) {
//                    // deliver when we arrive
//                    pm.deliver(this, curr, time);
//                    parcels.remove(0);
//                }
//            } else {
//                // it is still available, go there as fast as possible
//                rm.moveTo(this, curr, time);
//                if (rm.equalPosition(this, curr)) {
//                    // pickup customer
//                    pm.pickup(this, curr, time);
//                }
//            }
//        }
    }


    @Override
    public Optional<Point> getPosition() {
        if (this.getRoadModel().get().containsObject(this)) {
            return Optional.of(this.getRoadModel().get().getPosition(this));
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
