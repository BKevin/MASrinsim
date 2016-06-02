package mas.cbba.agent;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import mas.MyParcel;
import mas.MyVehicle;
import mas.cbba.Debug;
import mas.cbba.parcel.MultiParcel;
import mas.cbba.snapshot.CbbaSnapshot;
import mas.cbba.snapshot.Snapshot;
import mas.comm.NewParcelMessage;
import mas.comm.ParcelMessage;
import mas.comm.SoldParcelMessage;
import mas.route.evaluation.RouteTimes;
import mas.route.evaluation.strategy.TotalCostValue;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by pieter on 30.05.16.
 */
public abstract class AbstractConsensusAgent extends MyVehicle {

    public static final Long NO_BID = Long.MAX_VALUE; //TODO check of dit overal klopt


    private LinkedList<Parcel> b;
    private ArrayList<Parcel> p;

    private Map<AbstractConsensusAgent, Long> communicationTimestamps;

    // Previous snapshot
    private Snapshot snapshot;

    //Statistics fields
    private int numberOfSentMessages;
    private int numberOfReceivedMessages;
    private int numberOfRouteCostCalculations;
    private int numberOfConstructBundleCalls;
    private double averageAvailableParcels;
    private double averageClaimedParcels;

    public AbstractConsensusAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);
        this.p = new ArrayList<>();
        this.b = new LinkedList<>();
        this.communicationTimestamps = new HashMap<>();
        numberOfSentMessages = 0;
        numberOfReceivedMessages = 0;
        numberOfRouteCostCalculations = 0;
        numberOfConstructBundleCalls = 0;
        averageAvailableParcels = 0;
        averageClaimedParcels = 0;
    }

    @Override
    public void afterTick(TimeLapse time) {
        super.afterTick(time);
//        LoggerFactory.getLogger(this.getClass()).info("AfterTick #messages = {}", this.getCommDevice().get().getUnreadCount());

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
            numberOfConstructBundleCalls += 1;
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
        calculateAverages(time);


    }

    private void calculateAverages(TimeLapse time) {
        //Calculate average available parcel
        int currentAvailable = this.getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE).size();
        currentAvailable += this.getPDPModel().getParcels(PDPModel.ParcelState.ANNOUNCED).size();
        long temp =  (long)(averageAvailableParcels * (time.getEndTime()/time.getTickLength() - 1));
        temp += currentAvailable;
        averageAvailableParcels = ((double)temp) / (time.getEndTime()/time.getTickLength());
        //Calculate average claimed parcel
        long temp2 =  (long)(averageClaimedParcels * (time.getEndTime()/time.getTickLength() - 1));
        temp2 += this.getB().size();
        averageClaimedParcels = ((double)temp2) / (time.getEndTime()/time.getTickLength());
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
                    this.numberOfSentMessages += 1;
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

    public void updateRoute() {

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
        this.numberOfRouteCostCalculations += 1;

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
            this.numberOfReceivedMessages += 1;

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

    protected abstract AbstractConsensusAgent getWinningAgentBy(Parcel parcel);

    protected abstract Long getWinningBidBy(Parcel parcel);


    /***************************
     *
     * CBBA algorithm
     *
     ****************************/

    /**
     * Evaluate a single snapshot message from another sender
     */
    public void evaluateSnapshot(Snapshot s, AbstractConsensusAgent sender){
//        if(!(s instanceof CbbaSnapshot)){
//            throw new IllegalArgumentException("Snapshot does not have the right format. Expected CbbaSnapshot");
//        }

//        CbbaSnapshot s = (CbbaSnapshot) s;



        // TODO Original Cbba Table for bid evaluation.
//        CbbaSnapshot mySnapshot = (CbbaSnapshot) this.getSnapshot();

        for(Parcel parcel : this.getParcels()){
            //If the incoming snapshot has no information about this parcel, continue to the next one.
            if(s.getWinningAgentBy(parcel) == null){
                continue;
            }
            AbstractConsensusAgent myIdea = this.getWinningAgentBy(parcel);
            AbstractConsensusAgent otherIdea = s.getWinningAgentBy(parcel);

            if(sender.equals(otherIdea)){
                senderThinksHeWins(sender, parcel, myIdea, s);
                continue;
            }
            if(this.equals(otherIdea)){
                senderThinksIWin(sender, parcel, myIdea, s);
                continue;
            }
            if(otherIdea != null && !sender.equals(otherIdea) && !this.equals(otherIdea)){
                senderThinksSomeoneElseWins(sender, parcel, myIdea, otherIdea, s);
                continue;
            }
            if(otherIdea == null){
                senderThinksNododyWins(sender, parcel, myIdea, s);
                continue;
            }


        }

    }


    private void senderThinksHeWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, Snapshot otherSnapshot) {
        //I think I win
        if(this.equals(myIdea)){
            if(compareBids(otherSnapshot.getWinningBidBy(parcel),sender,this.getWinningBidBy(parcel),this))
                update(parcel, otherSnapshot);
            return;
        }
        //I think sender wins
        if(sender.equals(myIdea)){
            update(parcel, otherSnapshot);
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            Long otherTimeStamp = otherSnapshot.getCommunicationTimestamps().get(myIdea);
            Long myTimeStamp = this.getCommunicationTimestamps().get(myIdea);
            if((otherTimeStamp != null && otherTimeStamp > myTimeStamp)
                    || (compareBids(otherSnapshot.getWinningBidBy(parcel),sender,this.getWinningBidBy(parcel),myIdea)))
                update(parcel, otherSnapshot);
            return;
        }
        if(myIdea == null){
            update(parcel, otherSnapshot);
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksHeWins: unreachable code.");
    }

    private void senderThinksIWin(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, Snapshot otherSnapshot) {
        if(this.equals(myIdea)) {
            leave();
            return;
        }
        if(sender.equals(myIdea)){
            reset(parcel);
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            Long otherTimeStamp = otherSnapshot.getCommunicationTimestamps().get(myIdea);
            Long myTimeStamp = this.getCommunicationTimestamps().get(myIdea);
            if((otherTimeStamp != null && otherTimeStamp > myTimeStamp))
                reset(parcel);
            return;
        }
        if(myIdea == null){
            leave();
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksIWins: unreachable code.");
    }

    private void senderThinksSomeoneElseWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, AbstractConsensusAgent otherIdea, Snapshot otherSnapshot) {

        Long otherTimeStamp = otherSnapshot.getCommunicationTimestamps().get(otherIdea);
        Long myTimeStamp = this.getCommunicationTimestamps().get(otherIdea);
        boolean otherHasNewerSnapshotForM = myTimeStamp == null || otherTimeStamp > myTimeStamp;

        if(this.equals(myIdea)) {
            if(otherHasNewerSnapshotForM
                    && (compareBids(otherSnapshot.getWinningBidBy(parcel),otherIdea,this.getWinningBidBy(parcel),myIdea)))
                update(parcel, otherSnapshot);
            return;
        }
        if(sender.equals(myIdea)){
            if(otherHasNewerSnapshotForM)
                update(parcel, otherSnapshot);
            else
                reset(parcel);
            return;
        }
        if(otherIdea.equals(myIdea)){
            if(otherHasNewerSnapshotForM)
                update(parcel, otherSnapshot);
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea) && !otherIdea.equals(myIdea)){

            Long otherTimeStampMy = otherSnapshot.getCommunicationTimestamps().get(myIdea);
            Long myTimeStampMy = this.getCommunicationTimestamps().get(myIdea);
            boolean otherHasNewerSnapshotForN = otherTimeStampMy != null && otherTimeStampMy >myTimeStampMy;


            if(otherHasNewerSnapshotForM
                    && otherHasNewerSnapshotForN)
                update(parcel, otherSnapshot);
            if(otherHasNewerSnapshotForM
                    && (compareBids(otherSnapshot.getWinningBidBy(parcel),otherIdea,this.getWinningBidBy(parcel),myIdea)))
                update(parcel, otherSnapshot);
            if(otherHasNewerSnapshotForN
                    && !otherHasNewerSnapshotForM)
                reset(parcel);
            return;
        }
        if(myIdea == null){
            if(otherHasNewerSnapshotForM)
                update(parcel, otherSnapshot);
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksSomeoneElseWins: unreachable code.");
    }
    private void senderThinksNododyWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, Snapshot otherSnapshot) {
        if(this.equals(myIdea)) {
            leave();
            return;
        }
        if(sender.equals(myIdea)){
            update(parcel, otherSnapshot);
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            Long otherTimeStamp = otherSnapshot.getCommunicationTimestamps().get(myIdea);
            Long myTimeStamp = this.getCommunicationTimestamps().get(myIdea);
            if((otherTimeStamp != null && otherTimeStamp > myTimeStamp))
                update(parcel, otherSnapshot);
            return;
        }
        if(myIdea == null){
            leave();
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksNododyWins: unreachable code.");
    }

    private boolean compareBids(long bid1, AbstractConsensusAgent agent1, long bid2, AbstractConsensusAgent agent2){
        return isBetterBidThan(bid1,bid2)
                || (bid1 == bid2 && agent1.hashCode() > agent2.hashCode());
    }

    private void update(Parcel parcel, Snapshot snapshot) {
        this.setWinningBid(parcel, snapshot.getWinningAgentBy(parcel), snapshot.getWinningBidBy(parcel));
    }

    private void leave() {
        //do nothing
    }

    private void reset(Parcel parcel) {
        this.setWinningBid(parcel, this, NO_BID);
    }


    public abstract Set<Parcel> getParcels();

    public Integer getNumberOfSentMessages() {
        return numberOfSentMessages;
    }

    public Integer getNumberOfReceivedMessages() {
        return numberOfReceivedMessages;
    }

    public int getNumberOfRouteCostCalculations() {
        return numberOfRouteCostCalculations;
    }

    public int getNumberOfConstructBundleCalls() {
        return numberOfConstructBundleCalls;
    }

    public double getAverageAvailableParcels() {
        return averageAvailableParcels;
    }

    public double getAverageClaimedParcels() {
        return averageClaimedParcels;
    }
}
