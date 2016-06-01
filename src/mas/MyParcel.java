package mas;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;
import mas.comm.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by KevinB on 8/05/2016.
 */
public class MyParcel extends Parcel implements CommUser, TickListener{

    public static int DEFAULT_REQUIRED_AGENTS = 1;

    private Optional<CommDevice> device;

//    private boolean auctioned;
//
//    private boolean broadcasted;
//    private List<Message> bids;
    // Time window and penalty information

    private boolean announcedArrival;
    private boolean announcedSold;

//    private final TimeWindowPolicy policy = TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED;

    // Delivery details
    private long pickUpTime = 0;
    private long deliverTime = 0;
    private List<Vehicle> allocatedVehicles;

    public MyParcel(ParcelDTO parcelDTO){
        super(parcelDTO);
        this.announcedArrival = false;
        this.announcedSold = false;
//        broadcasted = false;
//        bids = null;
        this.allocatedVehicles = new ArrayList<>();
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


    public Optional<CommDevice> getCommDevice() {
        return device;
    }

    public void tick(TimeLapse timeLapse) {

        if(!announcedArrival){
            ParcelMessage contents = new NewParcelMessage(this);
            this.getCommDevice().get().broadcast(contents);
            LoggerFactory.getLogger(this.getClass()).info("Broadcasted {} from {} : {}", NewParcelMessage.class, this, contents);
            setAnnouncedArrival();
        }


        dismissMessages();

    }

    /**
     * Ignore all incoming messages
     */
    private void dismissMessages() {
        for(Message m : this.getCommDevice().get().getUnreadMessages()){
            continue;
        }
    }

    protected void setAnnouncedArrival(){ this.announcedArrival = true; }
    protected void setAnnouncedSold(){
        this.announcedSold = true;
    }

//    @Override
//    public void tick(TimeLapse timeLapse) {
//        if (auctioned){
//            return;
//        }
//        // if not yet broadcasted, broadcast
//        if (!broadcasted){
//            device.get().broadcast(new AuctionedParcelMessage(this));
//            bids = new ArrayList<Message>();
//            broadcasted = true;
//            return;
//        }
//        //check incoming messages
//        if (device.get().getUnreadCount() > 0) {
//            ImmutableList<Message> messages = device.get().getUnreadMessages();
//            for(Message message : messages){
//                MessageContents contents = message.getContents();
//                //if BidMessage then save
//                if(contents instanceof BidMessage){
//                    BidMessage bidContent = (BidMessage) contents;
//                    bids.add(message);
//                }
//            }
//        }
//
//        if(bids.size() < this.getPDPModel().getVehicles().size()) {
//            //wait until all bids are done
//            return;
//        }
//
//        //handle ending auction:
//        endAuction();
//
//    }

//    private void endAuction(){
//
//        Message m = getBestBid();
//
//        List<Message> losingbids = new ArrayList<>();
//        Collections.copy(losingbids, bids);
//        losingbids.remove(m);
//
//        //send AcceptBidMessage to winner of auction
//        device.get().send(new AcceptBidMessage(this), m.getSender());
//
//        this.allocateTo(((BidMessage)m.getContents()).getVehicle());
//
//        //send RefuseBidMessage to losers
//        for (Message message : losingbids) {
//            device.get().send(new RefuseBidMessage(this), message.getSender());
//        }
//
//        auctioned = true;
//    }

//    private Message getBestBid(){
//        //find the best bid
//        // use ThreadLocalRandom, as per http://stackoverflow.com/a/363692
//        Message bestMess = bids.get(ThreadLocalRandom.current().nextInt(bids.size()));
//        for (Message message : bids) {
//            BidMessage bestBid = (BidMessage) bestMess.getContents();
//            BidMessage bid = (BidMessage) message.getContents();
//            if (false) { //TODO compare bids
//                bestMess = message;
//                throw new NotImplementedException();
//            }
//        }
//
//        return bestMess;
//    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

        PDPModel.ParcelState state = this.getPDPModel().getParcelState(this);
        if(!announcedSold
                && !state.equals(PDPModel.ParcelState.ANNOUNCED)
                && !state.equals(PDPModel.ParcelState.AVAILABLE)){
            this.getCommDevice().get().broadcast(new SoldParcelMessage(this));
            this.setAnnouncedSold();
        }
    }

    @Override
    public boolean canBePickedUp(Vehicle v, long time){
        // Timewindowpolicy is already check before pickup
        // DefaultPDPModel#pickup:194
//        LoggerFactory.getLogger(this.getClass()).info("canBePickedUp = {} -> expect: {} and actual: {}",this.vehicle == v, this.vehicle, v);

//        return this.isAllocated() && this.allocatedVehicles.contains(v);
        return this.allocatedVehicles.contains(v);
    }

    @Override
    public boolean canBeDelivered(Vehicle v, long time) {
        //Timewindowpolicy is already checked before delivery
        // DefaultPDPModel#deliver:282
//        return this.isAllocated() && this.allocatedVehicles.contains(v);
        return true;
    }

    /**
     * Penalty methods, depend on recorded pickup and delivery time.
     */

    /**
     * Change state of parcel to "picked up"
     * @param v
     * @param time
     */
    public void pickUp(Vehicle v, long time) {
        if (!this.canBePickedUp(v, time)){
            throw new IllegalStateException("Parcel cannot be picked up at time " + time + " by vehicle " + v.toString());
        }
//        if(this.isPickedUp()){
//            throw new IllegalStateException("Parcel is already picked up.");
//        }

        this.pickUpTime = time;
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
//        if(this.isDelivered()){
//            throw new IllegalStateException("Parcel is already deliverd.");
//        }

        this.deliverTime = time;
    }

    public boolean isPickedUp(){
        return this.pickUpTime > 0;
    }

    public boolean isDelivered(){
        return this.deliverTime > 0;
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
        TimeWindow pickupTimeWindow = this.getPickupTimeWindow();
        long end = pickupTimeWindow.end();
        return pickupTime - end > 0
                ? pickupTime - end
                : 0L;
    }

    /**
     * Allocation methods
     */

    /**
     * @return Whether this parcel is allocated to a vehicle
     */
    public boolean isAllocated() {
        return this.allocatedVehicles.size() == 1;
    }

    public Vehicle getAllocatedVehicle() {
        if(isAllocated())
            return this.allocatedVehicles.get(0);
        else
            return null;
    }

    public Parcel allocateTo(Vehicle vehicle) {
        if(!this.getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE, PDPModel.ParcelState.ANNOUNCED).contains(this) ){
            throw new IllegalStateException("Parcel cannot be transferred anymore: " + this);
        }
        this.allocatedVehicles.add(vehicle);
        return this;
    }

    public Parcel loseAllocation(Vehicle vehicle){
        this.allocatedVehicles.remove(vehicle);
        return  this;
    }

    protected Parcel changeAllocation(Vehicle from, Vehicle to){
        if(from != this.getAllocatedVehicle()){
            throw new IllegalArgumentException("'From' vehicle does not match current allocation.");
        }
        // FIXME check if SubParcel method correctly calls this.
        return this.allocateTo(to);
    }

    public Integer getRequiredAgents(){
        return DEFAULT_REQUIRED_AGENTS;
    }
}
