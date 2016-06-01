package mas.cbba.agent;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import mas.MyParcel;
import mas.MyVehicle;
import mas.cbba.Debug;
import mas.cbba.parcel.MultiParcel;
import mas.cbba.snapshot.Snapshot;
import mas.comm.NewParcelMessage;
import mas.comm.ParcelMessage;
import mas.comm.SoldParcelMessage;
import mas.route.evaluation.RouteTimes;
import mas.route.evaluation.strategy.TotalCostValue;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by pieter on 30.05.16.
 */
public abstract class AbstractConsensusAgent extends MyVehicle {


    private LinkedList<Parcel> b;
    private ArrayList<Parcel> p;

    private Map<AbstractConsensusAgent, Long> communicationTimestamps;

    // Previous snapshot
    private Snapshot snapshot;

    private boolean inTick;
    private boolean error = false;

    public AbstractConsensusAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);
        this.p = new ArrayList<>();
        this.b = new LinkedList<>();
        this.communicationTimestamps = new HashMap<>();
    }

    protected void preTick(TimeLapse time) {
        super.preTick(time);
//        inTick = true;
//
//        if(!this.getPDPModel().getContents(this).isEmpty())
//            LoggerFactory.getLogger(this.getClass()).info("At Start Contents of {} = {}  with State = {}", this, this.getPDPModel().getContents(this), this.getPDPModel().getParcelState(this.getPDPModel().getContents(this).iterator().next()));
//        LoggerFactory.getLogger(this.getClass()).info("At Start Route of {} = {} ", this, this.getRoute());
//
//        org.slf4j.LoggerFactory.getLogger(this.getClass()).warn("Pretick start for {}", this);

        //"Realtime" implementatie: verander de while loop door een for loop of asynchrone thread,
        // iedere tick wordt er dan een beperkte berekening/communicatie gedaan.

        boolean hasConsensus = false;
        while (!hasConsensus){
            constructBundle();
            hasConsensus = findConsensus();

//            org.slf4j.LoggerFactory.getLogger(this.getClass()).warn("Pretick %s, %s", this, this.getP().size());
        }

        //Route mending

        if(!this.getPDPModel().getContents(this).isEmpty()
                && !this.getRoute().contains(this.getPDPModel().getContents(this).iterator().next())){
            updateRoute();
        }

//        org.slf4j.LoggerFactory.getLogger(this.getClass()).warn("Pretick done for {}", this);

//        LoggerFactory.getLogger(this.getClass()).info("Route size: {} for {} ", this.getRoute().size(), this);
//        if(!this.getPDPModel().getContents(this).isEmpty())
//            LoggerFactory.getLogger(this.getClass()).info("At End Contents of {} = {}  with State = {}", this, this.getPDPModel().getContents(this), this.getPDPModel().getParcelState(this.getPDPModel().getContents(this).iterator().next()));
//        LoggerFactory.getLogger(this.getClass()).info("At End Route of {} = {} ", this, this.getRoute());
//            try{
//                throw new IllegalStateException("Bad Route" + error);
//            }
//            catch(Exception e) {
//                LoggerFactory.getLogger(this.getClass()).warn("Bad route", e
//                );
//            }
//        inTick = false;
    }

    @Override
    public void setRoute(Iterable<? extends Parcel> r) {
        super.setRoute(r);
//        try{
//            throw new ConcurrentModificationException("Printing Route stacktrace");
//        }
//        catch(Exception e){
//            e.printStackTrace();
//        }
//        if(r.iterator().hasNext()
//                && !this.getPDPModel().getContents(this).isEmpty()
//                && !this.getPDPModel().getContents(this).contains(r.iterator().next()))
//            throw new IllegalStateException("Bad Route set");
//        if(!inTick){
//            error = true;
//            throw new IllegalStateException("SetRoute happened outside tick");}
    }

    public abstract void constructBundle();

    public abstract void evaluateSnapshot(Snapshot snaphot, AbstractConsensusAgent k);

    protected abstract Snapshot generateSnapshot();

    public LinkedList<Parcel> getB() {
        return b;
    }

    public ArrayList<Parcel> getP() {
        return p;
    }

    protected Snapshot getSnapshot() {
        return snapshot;
    }

    protected void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    /**
     * Set timestamp to last received message timestamp for that agent
     * @param agent
     * @param time
     */
    private void setCommunicationTimestamp(AbstractConsensusAgent agent, Long time){
        this.communicationTimestamps.put(agent, time);
    }

    /**
     * Set timestamp to last received message timestamp for the sending agent.
     * @param message Snapshot Message from a AbstractConsensusAgent
     */
    protected void setCommunicationTimestamp(Message message){

        Snapshot snapshot = (Snapshot) message.getContents();
        AbstractConsensusAgent agent = (AbstractConsensusAgent) message.getSender();

        this.setCommunicationTimestamp(agent, snapshot.getTimestamp());
    }

    public Map<AbstractConsensusAgent, Long> getCommunicationTimestamps() {
        return ImmutableMap.copyOf(communicationTimestamps);
    }

    protected void sendSnapshot(Snapshot snapshot){
        // If the current information is different from the information we sent last time, resend.
        //FIXME equals
        if(!snapshot.equals(this.getSnapshot())){

            this.setSnapshot(snapshot);

            //TODO getVehicles: send to agent k with g_ik(t) = 1.
            for(Vehicle c : this.getPDPModel().getVehicles()) {
                if(c != this) {
                    MyVehicle v = (MyVehicle) c;
                    this.getCommDevice().get().send(snapshot, v);
                }
            }

            LoggerFactory.getLogger(this.getClass()).info("Sent snapshot from {},  {}", this, snapshot);
        };
    }

    protected void replaceWinningBid(Parcel parcel, AbstractConsensusAgent from, AbstractConsensusAgent to, Long bid){
        this.setWinningBid(parcel, to, bid);
    }

    /**
     * Set winning bid value for the given Parcel and AbstractConsensusAgent
     * @param parcel
     * @param agent
     * @param bid
     */
    protected void setWinningBid(Parcel parcel, AbstractConsensusAgent agent, Long bid){
        this.allocateParcelToWinner(parcel, agent);
    }

    /**
     * Change allocation of the given parcel to the given agent.
     * @param parcel
     * @param agent
     */
    protected void allocateParcelToWinner(Parcel parcel, AbstractConsensusAgent agent){
        if(!(parcel instanceof MyParcel)) {
            throw new IllegalArgumentException("Parcel is not the right type. Expected "+ MyParcel.class.getName());
        }

        //Consistency for agent routes when allocating own parcels
        if(agent == this) {

            PDPModel.ParcelState state = this.getPDPModel().getParcelState(parcel);
            if(state.equals(PDPModel.ParcelState.AVAILABLE) || state.equals(PDPModel.ParcelState.ANNOUNCED)) {
                Parcel p = ((MyParcel) parcel).allocateTo(agent);

                updateRoute();
            }else{

                LoggerFactory.getLogger(this.getClass()).warn("Trying to reallocate unavailable parcel {} (state: {}). Allocated to {}", parcel, state, ((MyParcel) parcel).getAllocatedVehicle());

            }


        }
        else{
            //Handle the loss of a parcel (since you (the previous winner) are replaced)
            if(this.getB().contains(parcel)){
                int ind = this.getB().indexOf(parcel);
                //make lists of parcels to keep and to remove
                ImmutableList<Parcel> newB = FluentIterable.from(this.getB()).limit(ind).toList();
                ImmutableList<Parcel> removedFromB = FluentIterable.from(this.getB()).skip(ind+1).toList(); //Do take 'parcel' as it should be already handled

                this.handleLostParcels(parcel, removedFromB);

            }
        }

    }

    /**
     * Subclasses need to handle the loss of parcels due to the loss of an earlier assigned parcel.
     * @param parcels
     */
    protected void handleLostParcels(Parcel cause, List<Parcel> parcels){

        if(cause instanceof MyParcel)
            ((MyParcel) cause).loseAllocation(this);
        for(Parcel parcel : parcels){
            if(parcel instanceof MyParcel)
                ((MyParcel) parcel).loseAllocation(this);
        }

        this.getP().remove(cause);
        this.getB().remove(cause);
        this.getP().removeAll(parcels);
        this.getB().removeAll(parcels);

        //Update route
        updateRoute();
    }

    private void updateRoute() {

        // Fetch actual parcels (in case of MultiParcel and build route
        List<Parcel> collect = this.getP().stream().map(this::getAllocatedParcel).collect(Collectors.toList());

//        LoggerFactory.getLogger(this.getClass()).info("RouteUpdate for {} with {}", this, collect);

        Debug.logRouteForAgent(this, this.getRoute().stream().collect(Collectors.toMap((Parcel p) -> p, p -> this.getPDPModel().getParcelState(p), (p1, p2) -> p1)));


        // Update route
        List<Parcel> routeFrom = this.createRouteFrom(
                collect);

//        if(!routeFrom.isEmpty()
//                && !this.getPDPModel().getContents(this).isEmpty()
//                && !this.getPDPModel().getContents(this).contains(routeFrom.iterator().next()))
//            throw new IllegalStateException("Bad Route");

        this.setRoute(
                // Create route with double parcels
                routeFrom);
    }

    private Parcel getAllocatedParcel(Parcel p){
        if(p instanceof MultiParcel){
            return ((MultiParcel) p).getAllocatedSubParcel(this);
        }
        else{
            return p;
        }
    }

    /**
     * Double all parcels in the given list and check for consistency
     * @param parcels
     * @return
     */
    private List<Parcel> createRouteFrom(List<Parcel> parcels){

        List<Parcel> result = new LinkedList<Parcel>();

        // Check for parcels that are picked up but not delivered yet.
        ImmutableSet<Parcel> currentContents = this.getPDPModel().getContents(this);

//        if(this.getPDPModel().getVehicleState(this).equals(PDPModel.VehicleState.PICKING_UP)){
//            if(currentContents.isEmpty())
//                result.add(this.getRoute().iterator().next());
//        }

        if(!currentContents.isEmpty()) {
            // A parcel is already picked up and must be added.
            for (Parcel p : currentContents){
                // correct if you have max 1 package in load.
                result.add(0, p);
            }
        }

        for(Parcel p : parcels){
            if(!currentContents.contains(p)){
                result.add(p);
                result.add(p);
            }
        }

        return result;
    }

    protected Long calculateBestRouteWith(Parcel parcel) {

        // Calculate minimum penalty
        return calculateRouteCostWith(parcel).stream().min(Long::compareTo).get();
    }

    protected List<Long> calculateRouteCostWith(Parcel parcel){
        // Map route calc for every index position
        List<Long> penalties = this.getP().stream().map((Parcel p) -> calculateRouteCostAtPosition(this.getP(), parcel, this.getP().indexOf(p))).collect(Collectors.toList());
        // Add extra route when adding at the end.
        penalties.add(calculateRouteCostAtPosition(this.getP(), parcel, this.getP().size()));

        return penalties;
    }

    protected long calculateRouteCostAtPosition(ArrayList<? extends Parcel> path, Parcel parcel, int positionOfParcel) {

        ArrayList<Parcel> adaptedPath = new ArrayList<Parcel>(path);
        adaptedPath.add(positionOfParcel,parcel);

        long newPathValue = calculateRouteCost(adaptedPath);

        // FIXME should be cached somewhere
        long oldPathValue = calculateRouteCost(this.getP());

//        LoggerFactory.getLogger(this.getClass()).info("CalculateRouteCostAtPosition{}: \nParcel {}, \nPath{}", positionOfParcel, parcel, path);

        // return difference
        return newPathValue - oldPathValue;
    }

    protected long calculateRouteCost(ArrayList<? extends Parcel> path) {
        RouteTimes routeTimes = new RouteTimes(
                this.getPDPModel(),
                this,
                new ArrayList<Parcel>(path),
                this.getProjectedStartPosition(),
                this.getProjectedStartTime(),
                this.getCurrentTimeLapse().getTimeUnit());

        return routeTimes.getValue(new TotalCostValue());
    }

    /**
     * Projected position is the position of the vehicle after the parcel in cargo has been delivered
     * @return The delivery position of the parcel in cargo or the current position if there is no parcel in cargo
     */
    private Point getProjectedStartPosition() {
        return this.getPDPModel().getContents(this).isEmpty()
                ? this.getPosition().get()
                : new ArrayList<Parcel>(this.getPDPModel().getContents(this)).get(0).getDeliveryLocation();
    }

    /**
     * Projected time is the moment that the vehicle may assume other tasks. If there are tasks in cargo they will
     * have to be completed first. Their completion time is added to the current time.
     * @return The delivery time of the parcel in cargo or the current time if there is no parcel in cargo
     */
    private Long getProjectedStartTime(){
        // FIXME does not include Delivering Time

        if(!this.getPDPModel().getContents(this).isEmpty()){
            long travelTimeToDestination = this.computeTravelTimeFromTo(
                    this.getPosition().get(),
                    getProjectedStartPosition(),
                    this.getCurrentTimeLapse().getTimeUnit());
            long arrivalTime = this.getCurrentTime() + travelTimeToDestination;
            Parcel parcel = new ArrayList<Parcel>(this.getPDPModel().getContents(this)).get(0);
            return parcel.canBeDelivered(this, arrivalTime) ? arrivalTime : parcel.getDeliveryTimeWindow().begin();
        }

        return this.getCurrentTime();
    }

    public Integer calculateBestRouteIndexWith(Parcel parcel) {
        List<Long> penalties = calculateRouteCostWith(parcel);
        return penalties.indexOf(penalties.stream().min(Long::compareTo).get());
    }

    protected boolean isBetterBidThan(double bid, double otherBid) {
        return bid < otherBid;
    }

    public boolean findConsensus() {

        // Send snapshot to all agents
        // Construct snapshot message
        //TODO kan ook via this.getCurrentTime(), geeft rechtstreeks long value.
        boolean hasNewInformation = this.getCommDevice().get().getUnreadCount() > 0;

        this.sendSnapshot(this.generateSnapshot());

        this.evaluateMessages();

        return !hasNewInformation;
    }

    /**
     * Evaluate received message from all agents
     */
    protected void evaluateMessages() {

        List<Message> snapshots = new LinkedList<>();
        for (Message message : this.getCommDevice().get().getUnreadMessages()) {

            //if AuctionedParcelMessage then calculate bid and send BidMessage
            final MessageContents contents = message.getContents();

            CommUser sender = message.getSender();

            // Received snapshot, update bid values.
            if (contents instanceof Snapshot) {
                snapshots.add(message);
            }

            if(contents instanceof SoldParcelMessage){
                this.removeParcel(((ParcelMessage) contents).getParcel());
                LoggerFactory.getLogger(this.getClass()).info("Received SoldParcelMessage from {} to {} : {}", sender, this, contents);
            }
            else if (contents instanceof NewParcelMessage){
                //TODO meer nodig?
                this.addParcel(((ParcelMessage) contents).getParcel());
                LoggerFactory.getLogger(this.getClass()).info("Received ParcelMessage from {} to {} : {}", sender, this, contents);
            }

        }

        for(Message snap : snapshots){
            this.setCommunicationTimestamp(snap);

            evaluateSnapshot((Snapshot) snap.getContents(), (AbstractConsensusAgent) snap.getSender());
//                LoggerFactory.getLogger(this.getClass()).info("Received Snapshot from {} to {} : {}", sender, this, contents);

        }

    }

    protected void removeParcel(Parcel parcel){
        this.getB().remove(parcel);
        this.getP().remove(parcel);
        this.updateRoute();
    };

    /**
     * Subclasses are given the new Parcel and can change their lists accordingly.
     * @param parcel
     */
    protected abstract void addParcel(Parcel parcel);

//    @Override
//    public Collection<Parcel> getRoute() {
//
//        LoggerFactory.getLogger(this.getClass()).info("RouteAccess {} {}: {}", this, this.getCurrentTime(), super.getRoute());
//        return super.getRoute();
//    }
}
