package main;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import main.comm.AcceptBidMessage;
import main.comm.AuctionedParcelMessage;
import main.comm.BidMessage;
import org.apache.commons.math3.random.RandomGenerator;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by KevinB on 8/05/2016.
 */
public class MyParcel extends Parcel implements CommUser, TickListener{

    private final RandomGenerator rng;
    private Optional<CommDevice> device;
    private boolean auctioned;
    private boolean broadcasted;
    private List<Message> bids;

    // Time window and penalty information

    private final TimeWindowPolicy policy = TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED;


    // Delivery details
    private long pickUpTime = 0;
    private long deliverTime = 0;
    private Vehicle vehicle;


    public MyParcel(ParcelDTO parcelDTO, RandomGenerator random) {
        super(parcelDTO);
        auctioned = false;
        broadcasted = false;
        bids = null;
        rng = random;
    }

    @Override
    public Optional<Point> getPosition() {
        if(this.getRoadModel().containsObject(this))
            return Optional.of(this.getRoadModel().getPosition(this));
        return Optional.absent();
    }


    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {

        device = Optional.of(commDeviceBuilder.build());
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (auctioned){
            return;
        }
        // if not yet broadcasted, broadcast
        if (!broadcasted){
            device.get().broadcast(new AuctionedParcelMessage(this));
            bids = new ArrayList<Message>();
            broadcasted = true;
            return;
        }
        //check incoming messages
        if (device.get().getUnreadCount() > 0) {
            ImmutableList<Message> messages = device.get().getUnreadMessages();
            for(Message message : messages){
                MessageContents contents = message.getContents();
                //if BidMessage then save
                if(contents instanceof BidMessage){
                    BidMessage bidContent = (BidMessage) contents;
                    bids.add(message);
                }
            }
        }

        if(bids.size() < this.getPDPModel().getVehicles().size()) {
            //wait until all bids are done
            return;
        }

        //handle ending auction:
        //find the best bid
        Message bestMess = bids.get(rng.nextInt(bids.size()));
        for (Message message : bids) {
            BidMessage bestBid = (BidMessage) bestMess.getContents();
            BidMessage bid = (BidMessage) message.getContents();
            if (false) { //TODO compare bids
                bestMess = message;
                throw new NotImplementedException();
            }
        }
        //send AcceptBidMessage to winner of auction
        MyVehicle winningVehicle = ((BidMessage) bestMess.getContents()).getVehicle();
        device.get().send(new AcceptBidMessage(this), winningVehicle);
        setWinningVehicle(winningVehicle);
        //send RefuseBidMessage to losers
        for (Message message : bids) {

        }

        auctioned = true;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }

    @Override
    public boolean canBePickedUp(Vehicle v, long time){
        return this.policy.canPickup(this.getPickupTimeWindow(), time, this.getPickupDuration()) && this.vehicle == v;
    }

    @Override
    public boolean canBeDelivered(Vehicle v, long time) {
        return this.policy.canDeliver(this.getDeliveryTimeWindow(), time, this.getDeliveryDuration()) && v == this.vehicle;
    }

    /**
     * Change state of parcel to "delivered"
     * @param v
     * @param time
     */
    public void deliver(Vehicle v, long time){
        if (!this.canBeDelivered(v, time)){
            throw new IllegalStateException("Parcel cannot be delivered at time " + time + " by vehicle " + v.toString());
        }
        if(this.isDelivered()){
            throw new IllegalStateException("Parcel is already deliverd.");
        }

        this.deliverTime = time;
    }

    /**
     * Change state of parcel to "picked up"
     * @param v
     * @param time
     */
    public void pickUp(Vehicle v, long time) {
        if (!this.canBePickedUp(v, time)){
            throw new IllegalStateException("Parcel cannot be picked up at time " + time + " by vehicle " + v.toString());
        }
        if(this.isPickedUp()){
            throw new IllegalStateException("Parcel is already picked up.");
        }

        this.pickUpTime = time;
    }

    public boolean isPickedUp(){
        return this.pickUpTime > 0;
    }

    public boolean isDelivered(){
        return this.deliverTime > 0;
    }

    public void setWinningVehicle(Vehicle winningVehicle) {
        this.vehicle = winningVehicle;
    }


    /**
     *  Compute the penalty incurred when picking up and delivering a parcel at the given time.
     */
    public long computePenalty(Long pickupTime, Long deliveryTime) {
        // TODO add caching to parcel penalty calculations

        return this.computePickupPenalty(pickupTime) + this.computeDeliveryPenalty(deliveryTime);
    }

    private long computeDeliveryPenalty(Long deliveryTime) {
        // Linear penalty calculation
        return deliveryTime - this.getDeliveryTimeWindow().end() > 0
                ? deliveryTime - this.getDeliveryTimeWindow().end()
                : 0;
    }

    private long computePickupPenalty(Long pickupTime) {
        // Linear penalty calculation
        return pickupTime - this.getPickupTimeWindow().end() > 0
                ? pickupTime - this.getPickupTimeWindow().end()
                : 0;
    }


}
