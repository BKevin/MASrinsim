package main;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import comm.AcceptBidMessage;
import comm.AuctionedParcelMessage;
import comm.BidMessage;

import java.sql.Time;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by KevinB on 8/05/2016.
 */
public class MyVehicle extends Vehicle implements CommUser{

    private Optional<RoadModel> roadModel;
    private Optional<PDPModel>  pPdpModel;
    private Optional<CommDevice> device;
    private List<MyParcel> parcels;

    public MyVehicle(Point startPosition, int vehicleCapacity, double speed) {
        super(VehicleDTO.builder()
                .startPosition(startPosition)
                .capacity(vehicleCapacity)
                .speed(speed)
                .build());
        parcels = new LinkedList<MyParcel>();
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel ppPdpModel) {
        roadModel = Optional.of(pRoadModel);
        pPdpModel = Optional.of(ppPdpModel);
    }

    @Override
    protected void tickImpl(TimeLapse timeLapse) {

        communicate();

        move(timeLapse);

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
                AcceptBidMessage acceptedBidMessage = (AcceptBidMessage) contents;
                parcels.add(acceptedBidMessage.getParcel());
                haveToRecalculate = true;
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


    private void move(TimeLapse time) {
        //move in the direction of the first scheduled parcel
        //if arrived then pickup or deliver Parcel
        final RoadModel rm = getRoadModel();
        final PDPModel pm = getPDPModel();

        if (!time.hasTimeLeft()) {
            return;
        }

        if (parcels.isEmpty()) {
            //do nothing when no assignment
            return;
        }

        if (!parcels.isEmpty()) {
            MyParcel curr = parcels.get(0);
            final boolean inCargo = pm.containerContains(this, curr);
            // sanity check: if it is not in our cargo AND it is also not on the
            // RoadModel, we cannot go to curr anymore.
            if (!inCargo && !rm.containsObject(curr)) {
                throw new IllegalStateException("A Parcel isn't available, but MyVehicle still has to PDP it.");
            } else if (inCargo) {
                // if it is in cargo, go to its destination
                rm.moveTo(this, curr.getDeliveryLocation(), time);
                if (rm.getPosition(this).equals(curr.getDeliveryLocation())) {
                    // deliver when we arrive
                    pm.deliver(this, curr, time);
                    parcels.remove(0);
                }
            } else {
                // it is still available, go there as fast as possible
                rm.moveTo(this, curr, time);
                if (rm.equalPosition(this, curr)) {
                    // pickup customer
                    pm.pickup(this, curr, time);
                }
            }
        }
    }


    @Override
    public Optional<Point> getPosition() {
        if (roadModel.get().containsObject(this)) {
            return Optional.of(roadModel.get().getPosition(this));
        }
        return Optional.absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        device = Optional.of(commDeviceBuilder.build());
    }



    public List<MyParcel> getPlanning(){
        return this.parcels;
    }

    public void getPenalty(){

    }


}
