package main.cbba.agent;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.ImmutableMap;
import main.MyParcel;
import main.MyVehicle;
import main.cbba.snapshot.CbbaSnapshot;
import main.cbba.snapshot.Snapshot;
import main.route.evaluation.RouteEvaluation;
import main.route.evaluation.RouteTimes;

import java.util.*;

/**
 * Created by pieter on 26.05.16.
 */
public class CbbaVehicle extends MyVehicle implements ConsensusAgent {

    private LinkedList<Parcel> b;
    private ArrayList<Parcel> p;
    private Map<Parcel, Long> y;
    private Map<Parcel, ConsensusAgent> z;

    private Map<ConsensusAgent, Long> communicationTimestamps;

    // Previous snapshot
    private Snapshot snapshot;


    public CbbaVehicle(VehicleDTO vehicleDTO) {
        super(vehicleDTO);
        this.communicationTimestamps = new HashMap<>();

        this.b = new LinkedList<>();
        this.p = new ArrayList<>();
        this.y = new HashMap<>();
        this.z = new HashMap<>();
    }

    public Map<ConsensusAgent, Long> getCommunicationTimestamps() {
        return ImmutableMap.copyOf(communicationTimestamps);
    }

    /**
     * Set timestamp to last received message timestamp for that agent
     * @param agent
     * @param time
     */
    private void setCommunicationTimestamp(ConsensusAgent agent, Long time){
        this.communicationTimestamps.put(agent, time);
    }

    /**
     * Set timestamp to last received message timestamp for the sending agent.
     * @param message Snapshot Message from a ConsensusAgent
     */
    protected void setCommunicationTimestamp(Message message){

        Snapshot snapshot = (Snapshot) message.getContents();
        ConsensusAgent agent = (ConsensusAgent) message.getSender();

        this.setCommunicationTimestamp(agent, snapshot.getTimestamp());
    }


    @Override
    public void constructBundle() {
        LinkedList<? extends Parcel> newB = getB();
        ArrayList<? extends Parcel> newP = getP();
        Map<? extends Parcel, Long> newY = getY(); //FIXME it now uses Immutablemaps
        Map<? extends Parcel, ConsensusAgent> newZ = getZ(); //FIXME it now uses Immutablemaps

        long currentPenalty = calculatePenalty(newP);

        boolean bIsChanging = true;
        while(bIsChanging){
            bIsChanging = false;

            long bestBid = Long.MAX_VALUE;
            Parcel bestParcel = null;
            int bestPosition = -1;

            //look at all parcels
            for(Parcel parcel : newZ.keySet()){
                //if you don't own the parcel yet, check it
                if(!newZ.get(parcel).equals(this)){

                    for(int pos = 0; pos <= newP.size(); pos++){
                        //calculate a bid for each position
                        long bid = calculatePenaltyAtPosition(newP,parcel,pos) - currentPenalty; //TODO aftrekken of optellen? (beter aftrek functie in penalty)
                        //check if bid is better than current best
                        if(isBetterBidThan(bid,newY.get(parcel))){
                            //check if bid is better than previous best
                            if(isBetterBidThan(bid, bestBid)){
                                //If better, save appropriate info
                                bestBid = bid;
                                bestParcel = parcel;
                                bestPosition = pos;
                            }
                        }
                    }
                }
            }
            if(bestParcel != null){
                b.addLast( bestParcel);
                p.add(bestPosition, bestParcel);

                this.setWinningBid(bestParcel, this, bestBid);

                bIsChanging = true;
            }
        }
    }

    /**
     * Set winning bid value for the given Parcel and ConsensusAgent
     * @param parcel
     * @param agent
     * @param bid
     */
    protected void setWinningBid(Parcel parcel, ConsensusAgent agent, Long bid){
        this.y.put(parcel,bid);
        this.z.put(parcel,agent);

        ((MyParcel) parcel).allocateTo((Vehicle) agent);
    }

    protected Long calculateBestRouteWith(Parcel parcel) {
        throw new UnsupportedOperationException("Not implemented yet");
//        return 0D;
    }


    private long calculatePenaltyAtPosition(ArrayList<? extends Parcel> path, Parcel parcel, int positionOfParcel) {
        ArrayList<Parcel> adaptedPath = new ArrayList<Parcel>(path);
        adaptedPath.add(positionOfParcel,parcel);
        return calculatePenalty(adaptedPath);
    }

    private long calculatePenalty(ArrayList<? extends Parcel> path) {
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

    public LinkedList<Parcel> getB() {
        return b;
    }

    public ArrayList<Parcel> getP() {
        return p;
    }


    public Map<Parcel, Long> getY() {
        return ImmutableMap.copyOf(y);
    }

    public Map<Parcel, ConsensusAgent> getZ() {
        return ImmutableMap.copyOf(z);
    }


    @Override
    public void findConsensus() {

        // Send snapshot to all agents
        // Construct snapshot message
        //TODO kan ook via this.getCurrentTime(), geeft rechtstreeks long value.
        sendSnapshot(new CbbaSnapshot(this, this.getCurrentTimeLapse()));

        evaluateMessages();
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
        }
    }



    /**
     * Evaluate a single snapshot message from another sender
     */
    protected void evaluateSnapshot(Snapshot s){
        if(!(s instanceof CbbaSnapshot)){
            throw new IllegalArgumentException("Snapshot does not have the right format. Expected CbbaSnapshot");
        }

        CbbaSnapshot snapshot = (CbbaSnapshot) s;



        // TODO Original Cbba Table for bid evaluation.


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

    protected Snapshot getSnapshot() {
        return snapshot;
    }

    protected void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
    }
}
