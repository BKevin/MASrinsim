package main.cbba.agent;

import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.ImmutableMap;
import main.MyParcel;
import main.MyVehicle;
import main.cbba.parcel.MultiParcel;
import main.cbba.snapshot.Snapshot;
import main.route.evaluation.RouteEvaluation;
import main.route.evaluation.RouteTimes;
import org.apache.commons.math3.analysis.function.Abs;

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
        List<Parcel> current = this.getRoute().stream().collect(Collectors.toList());
        // Compare second element
        if(!current.get(1).equals(parcels.get(0))){
            // The first parcel is already picked up and may be deleted once.
            result.remove(0);
        }

        return result;
    }

    protected Long calculateBestRouteWith(Parcel parcel) {
        throw new UnsupportedOperationException("Not implemented yet");
//        return 0D;
    }

    protected long calculatePenaltyAtPosition(ArrayList<? extends Parcel> path, Parcel parcel, int positionOfParcel) {
        ArrayList<Parcel> adaptedPath = new ArrayList<Parcel>(path);
        adaptedPath.add(positionOfParcel,parcel);
        return calculatePenalty(adaptedPath);
    }

    protected long calculatePenalty(ArrayList<? extends Parcel> path) {
        RouteTimes routeTimes = new RouteTimes(this,new ArrayList<Parcel>(path),this.getPosition().get(),this.getCurrentTime(),this.getCurrentTimeLapse().getTimeUnit());
        RouteEvaluation evaluation = new RouteEvaluation(routeTimes);
        return evaluation.getPenalty().getRoutePenalty();
    }

    public Integer calculateBestRouteIndexWith(Parcel parcel) {
        return 0;
    }

    protected boolean isBetterBidThan(double bid, double otherBid) {
        return bid < otherBid;
    }


}
