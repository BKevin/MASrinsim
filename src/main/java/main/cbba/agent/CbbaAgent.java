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
     * Evaluate a single snapshot message from another sender
     */
    public void evaluateSnapshot(Snapshot s, AbstractConsensusAgent sender){
        if(!(s instanceof CbbaSnapshot)){
            throw new IllegalArgumentException("Snapshot does not have the right format. Expected CbbaSnapshot");
        }

        CbbaSnapshot otherSnapshot = (CbbaSnapshot) s;



        // TODO Original Cbba Table for bid evaluation.
        CbbaSnapshot mySnapshot = (CbbaSnapshot) this.getSnapshot();

        for(Parcel parcel : mySnapshot.getZ().keySet()){
            AbstractConsensusAgent myIdea = mySnapshot.getZ().get(parcel);
            AbstractConsensusAgent otherIdea = otherSnapshot.getZ().get(parcel);

            if(sender.equals(otherIdea)){
                senderThinksHeWins(sender, parcel, myIdea, mySnapshot, otherSnapshot);
                continue;
            }
            if(this.equals(otherIdea)){
                senderThinksIWin(sender, parcel, myIdea, mySnapshot, otherSnapshot);
                continue;
            }
            if(otherIdea != null && !sender.equals(otherIdea) && !this.equals(otherIdea)){
                senderThinksSomeoneElseWins(sender, parcel, myIdea, mySnapshot, otherIdea, otherSnapshot);
                continue;
            }
            if(otherIdea == null){
                senderThinksNododyWins(sender, parcel, myIdea, mySnapshot, otherSnapshot);
                continue;
            }


        }

    }

    private void senderThinksHeWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot mySnapshot, CbbaSnapshot otherSnapshot) {
        //I think I win
        if(this.equals(myIdea)){
            if(otherSnapshot.getY().get(parcel) > mySnapshot.getY().get(parcel))
                update();
            return;
        }
        //I think sender wins
        if(sender.equals(myIdea)){
            update();
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            if((otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                    || (otherSnapshot.getY().get(parcel) > mySnapshot.getY().get(parcel)))
                update();
            return;
        }
        if(myIdea == null){
            update();
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksHeWins: unreachable code.");
    }

    private void senderThinksIWin(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot mySnapshot, CbbaSnapshot otherSnapshot) {
        if(this.equals(myIdea)) {
            leave();
            return;
        }
        if(sender.equals(myIdea)){
            reset();
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                reset();
            return;
        }
        if(myIdea == null){
            leave();
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksHeWins: unreachable code.");
    }

    private void senderThinksSomeoneElseWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot mySnapshot, AbstractConsensusAgent otherIdea, CbbaSnapshot otherSnapshot) {
        if(this.equals(myIdea)) {
            if((otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                    && (otherSnapshot.getY().get(parcel) > mySnapshot.getY().get(parcel)))
                update();
            return;
        }
        if(sender.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update();
            else
                reset();
            return;
        }
        if(otherIdea.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update();
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea) && !otherIdea.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(otherIdea) > mySnapshot.getCommunicationTimestamps().get(otherIdea)
                    && otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update();
            if(otherSnapshot.getCommunicationTimestamps().get(otherIdea) > mySnapshot.getCommunicationTimestamps().get(otherIdea)
                    && (otherSnapshot.getY().get(parcel) > mySnapshot.getY().get(parcel)))
                update();
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea)
                    && mySnapshot.getCommunicationTimestamps().get(otherIdea) > otherSnapshot.getCommunicationTimestamps().get(otherIdea))
                reset();
            return;
        }
        if(myIdea == null){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update();
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksHeWins: unreachable code.");
    }

    private void senderThinksNododyWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot mySnapshot, CbbaSnapshot otherSnapshot) {
        if(this.equals(myIdea)) {
            leave();
            return;
        }
        if(sender.equals(myIdea)){
            update();
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update();
            return;
        }
        if(myIdea == null){
            leave();
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksHeWins: unreachable code.");
    }

}
