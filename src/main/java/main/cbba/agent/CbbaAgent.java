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
public class CbbaAgent extends AbstractConsensusAgent {

    private Map<Parcel, Long> y;
    private Map<Parcel, AbstractConsensusAgent> z;


    public CbbaAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);

        this.y = new HashMap<>();
        this.z = new HashMap<>();
    }

    public Map<Parcel, Long> getY() {
        return ImmutableMap.copyOf(y);
    }

    public Map<Parcel, AbstractConsensusAgent> getZ() {
        return ImmutableMap.copyOf(z);
    }

    public void constructBundle() {
        LinkedList<? extends Parcel> newB = getB();
        ArrayList<? extends Parcel> newP = getP();
        Map<? extends Parcel, Long> newY = getY(); //FIXME it now uses Immutablemaps
        Map<? extends Parcel, AbstractConsensusAgent> newZ = getZ(); //FIXME it now uses Immutablemaps

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
                getB().addLast( bestParcel);
                getP().add(bestPosition, bestParcel);

                this.setWinningBid(bestParcel, this, bestBid);

                bIsChanging = true;
            }
        }
    }


    /**
     * Set winning bid value for the given Parcel and AbstractConsensusAgent
     * @param parcel
     * @param agent
     * @param bid
     */
    @Override
    protected void setWinningBid(Parcel parcel, AbstractConsensusAgent agent, Long bid){
        super.setWinningBid(parcel, agent, bid);

        this.y.put(parcel,bid);
        this.z.put(parcel,agent);
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

}
