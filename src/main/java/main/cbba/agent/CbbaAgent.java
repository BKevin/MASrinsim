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
import java.util.stream.Collectors;

/**
 * Created by pieter on 26.05.16.
 */
public class CbbaAgent extends AbstractConsensusAgent {

    private static final Long NO_BID = Long.MAX_VALUE; //TODO check of dit overal klopt
    private Map<Parcel, Long> y;
    private Map<Parcel, AbstractConsensusAgent> z;


    public CbbaAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);

        this.y = new HashMap<>();
        this.z = new HashMap<>();
    }

    public void constructBundle() {

//        long currentPenalty = calculatePenalty(getP());


        Set<Parcel> parcels = this.z.keySet();
        // Get all parcels not already in B
        List<Parcel> notInB = parcels.stream().filter(p -> !this.getB().contains(p)).collect(Collectors.toList());

        boolean bIsChanging = true;
        while(bIsChanging){
            bIsChanging = false;

            long bestBid = NO_BID;
            Parcel bestParcel = null;
//            int bestPosition = -1;
            //look at all parcels
            for(Parcel parcel : notInB){
                //if you don't own the parcel yet, check it
                if(!this.z.get(parcel).equals(this)){
//
//                    for(int pos = 0; pos <= getP().size(); pos++){
//                        //calculate a bid for each position
//                        long bid = calculatePenaltyAtPosition(getP(),parcel,pos) - currentPenalty;
//                        //check if bid is better than current best
//                        if(isBetterBidThan(bid,newY.get(parcel))){
//                            //check if bid is better than previous best
//                            if(isBetterBidThan(bid, bestBid)){
//                                //If better, save appropriate info
//                                bestBid = bid;
//                                bestParcel = parcel;
//                                bestPosition = pos;
//                            }
//                        }
//                    }
                    long bid = this.calculateBestRouteWith(parcel);
                    //check if bid is better than current best
                    if(isBetterBidThan(bid,this.y.get(parcel))){
                        //check if bid is better than previous best
                        if(isBetterBidThan(bid, bestBid)){
                            //If better, save appropriate info
                            bestBid = bid;
                            bestParcel = parcel;
                        }
                    }
                }
            }
            if(bestParcel != null){
                getB().addLast(bestParcel);
//                getP().add(bestPosition, bestParcel);
                getP().add(this.calculateBestRouteIndexWith(bestParcel), bestParcel);

                this.setWinningBid(bestParcel, this, bestBid);

                bIsChanging = true;
            }
        }
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
                update(parcel, otherSnapshot);
            return;
        }
        //I think sender wins
        if(sender.equals(myIdea)){
            update(parcel, otherSnapshot);
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            if((otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                    || (otherSnapshot.getY().get(parcel) > mySnapshot.getY().get(parcel)))
                update(parcel, otherSnapshot);
            return;
        }
        if(myIdea == null){
            update(parcel, otherSnapshot);
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
            reset(parcel);
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                reset(parcel);
            return;
        }
        if(myIdea == null){
            leave();
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksIWins: unreachable code.");
    }

    private void senderThinksSomeoneElseWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot mySnapshot, AbstractConsensusAgent otherIdea, CbbaSnapshot otherSnapshot) {
        if(this.equals(myIdea)) {
            if((otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                    && (otherSnapshot.getY().get(parcel) > mySnapshot.getY().get(parcel)))
                update(parcel, otherSnapshot);
            return;
        }
        if(sender.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update(parcel, otherSnapshot);
            else
                reset(parcel);
            return;
        }
        if(otherIdea.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update(parcel, otherSnapshot);
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea) && !otherIdea.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(otherIdea) > mySnapshot.getCommunicationTimestamps().get(otherIdea)
                    && otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update(parcel, otherSnapshot);
            if(otherSnapshot.getCommunicationTimestamps().get(otherIdea) > mySnapshot.getCommunicationTimestamps().get(otherIdea)
                    && (otherSnapshot.getY().get(parcel) > mySnapshot.getY().get(parcel)))
                update(parcel, otherSnapshot);
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea)
                    && mySnapshot.getCommunicationTimestamps().get(otherIdea) > otherSnapshot.getCommunicationTimestamps().get(otherIdea))
                reset(parcel);
            return;
        }
        if(myIdea == null){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update(parcel, otherSnapshot);
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksSomeoneElseWins: unreachable code.");
    }
    private void senderThinksNododyWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot mySnapshot, CbbaSnapshot otherSnapshot) {
        if(this.equals(myIdea)) {
            leave();
            return;
        }
        if(sender.equals(myIdea)){
            update(parcel, otherSnapshot);
            return;
        }
        if(myIdea != null && !sender.equals(myIdea) && !this.equals(myIdea)){
            if(otherSnapshot.getCommunicationTimestamps().get(myIdea) > mySnapshot.getCommunicationTimestamps().get(myIdea))
                update(parcel, otherSnapshot);
            return;
        }
        if(myIdea == null){
            leave();
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksNododyWins: unreachable code.");
    }

    private void update(Parcel parcel, CbbaSnapshot snapshot) {
        this.setWinningBid(parcel, snapshot.getZ().get(parcel), snapshot.getY().get(parcel));
    }

    private void leave() {
        //do nothing
    }

    private void reset(Parcel parcel) {
        this.setWinningBid(parcel, null, NO_BID); //FIXME is dit legaal?
    }

    @Override
    protected void replaceWinningBid(Parcel parcel, AbstractConsensusAgent from, AbstractConsensusAgent to, Long bid){
        this.setWinningBid(parcel, to, bid);
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

        this.y.put(parcel, bid);
        this.z.put(parcel, agent);
    }

    @Override
    protected void handleLostParcels(List<Parcel> parcels) {
        //remove winning bids on the given parcels
        this.y.replaceAll(
                ((parcel, bid)
                        -> parcels.contains(parcel)
                        ? NO_BID
                        : bid));
        //remove winners of the given parcels
        this.z.replaceAll(
                ((parcel, winner)
                        -> parcels.contains(parcel)
                        ? null
                        : winner));
    }

    /**
     * Add the new parcel to the lists of bids and winners
     * @param parcel
     */
    @Override
    protected void addParcel(Parcel parcel) {
        this.y.put(parcel,NO_BID);
        this.z.put(parcel,null);
    }

    protected void removeParcel(Parcel parcel){
        this.y.remove(parcel);
        this.z.remove(parcel);
    }

    public Map<Parcel, Long> getY() {
        return ImmutableMap.copyOf(y);
    }

    public Map<Parcel, AbstractConsensusAgent> getZ() {
        return ImmutableMap.copyOf(z);
    }


}
