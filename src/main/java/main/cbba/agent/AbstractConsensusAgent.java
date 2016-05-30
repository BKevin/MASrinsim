package main.cbba.agent;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import main.MyParcel;
import main.MyVehicle;
import main.cbba.parcel.MultiParcel;
import main.cbba.snapshot.Snapshot;
import main.comm.ParcelMessage;
import main.route.evaluation.RouteEvaluation;
import main.route.evaluation.RouteTimes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by pieter on 30.05.16.
 */
public abstract class AbstractConsensusAgent extends MyVehicle {


    protected LinkedList<Parcel> b;
    protected ArrayList<Parcel> p;

    protected Map<AbstractConsensusAgent, Long> communicationTimestamps;

    // Previous snapshot
    private Snapshot snapshot;

    public AbstractConsensusAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);
        this.p = new ArrayList<>();
        this.b = new LinkedList<>();
        this.communicationTimestamps = new HashMap<>();
    }

    public abstract void constructBundle();

    public abstract void findConsensus();

    public abstract void evaluateSnapshot(Snapshot snaphot);

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
        if(!this.getSnapshot().equals(snapshot)){

            this.setSnapshot(snapshot);

            //TODO getVehicles: send to agent k with g_ik(t) = 1.
            for(Vehicle c : this.getPDPModel().getVehicles()) {
                MyVehicle v = (MyVehicle) c;
                this.getCommDevice().get().send(snapshot, v);
            }
        };
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

            Parcel p = ((MyParcel) parcel).allocateTo(agent);

            // Update route
            this.setRoute(
                    // Create route with double parcels
                    this.createRouteFrom(
                            // Fetch actual parcels (in case of MultiParcel and build route
                            this.getP().stream().map(this::getAllocatedParcel).collect(Collectors.toList())));
        }

    }

    private Parcel getAllocatedParcel(Parcel p){
        if(p instanceof MultiParcel){
            return ((MultiParcel) p).getAllocated(this);
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

        for(Parcel p : parcels){
            result.add(p);
            result.add(p);
        }

        // Check for parcels that are picked up but not delivered yet.
//        List<Parcel> current = this.getRoute().stream().collect(Collectors.toList());
        ImmutableSet<Parcel> currentContents = this.getPDPModel().getContents(this);

        if(!currentContents.isEmpty()){
                // A parcel is already picked up and must be added.
            for(Parcel p : currentContents)
                // correct if you have max 1 package in load.
                result.add(0, p);
        }

        return result;
    }

    protected Long calculateBestRouteWith(Parcel parcel) {

        // Calculate minimum penalty
        return calculatePenaltiesWith(parcel).stream().min(Long::compareTo).get();
    }

    protected List<Long> calculatePenaltiesWith(Parcel parcel){
        // Map route calc for every index position
        List<Long> penalties = this.getP().stream().map((Parcel p) -> calculatePenaltyAtPosition(this.getP(), parcel, this.getP().indexOf(p))).collect(Collectors.toList());
        // Add extra route when adding at the end.
        penalties.add(calculatePenaltyAtPosition(this.getP(), parcel, this.getP().size()));

        return penalties;
    }

    protected long calculatePenaltyAtPosition(ArrayList<? extends Parcel> path, Parcel parcel, int positionOfParcel) {
        ArrayList<Parcel> adaptedPath = new ArrayList<Parcel>(path);
        adaptedPath.add(positionOfParcel,parcel);

        long newPathValue = calculatePenalty(adaptedPath);
        // FIXME must be cached somewhere
        long oldPathValue = calculatePenalty(this.getP());

        // return difference
        return newPathValue - oldPathValue;
    }

    protected long calculatePenalty(ArrayList<? extends Parcel> path) {
        RouteTimes routeTimes = new RouteTimes(this,new ArrayList<Parcel>(path),this.getPosition().get(),this.getCurrentTime(),this.getCurrentTimeLapse().getTimeUnit());
        RouteEvaluation evaluation = new RouteEvaluation(routeTimes);

        return evaluation.getPenalty().getRoutePenalty();
    }

    public Integer calculateBestRouteIndexWith(Parcel parcel) {
        List<Long> penalties = calculatePenaltiesWith(parcel);
        return penalties.indexOf(penalties.stream().min(Long::compareTo).get());
    }

    protected boolean isBetterBidThan(double bid, double otherBid) {
        return bid < otherBid;
    }


    /**
     * Evaluate received message from all agents
     */
    protected void evaluateMessages() {

        for (Message message : this.getCommDevice().get().getUnreadMessages()) {

            //if AuctionedParcelMessage then calculate bid and send BidMessage
            final MessageContents contents = message.getContents();

            CommUser sender = message.getSender();

            // Received snapshot, update bid values.
            if (contents instanceof Snapshot) {
                this.setCommunicationTimestamp(message);

                evaluateSnapshot((Snapshot) message.getContents());
            }

//            if (contents instanceof ParcelMessage){
//
//                this.addParcel(((ParcelMessage) contents).getParcel());
//
//
//            }
        }
    }



}
