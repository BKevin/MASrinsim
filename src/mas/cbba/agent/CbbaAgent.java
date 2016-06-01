package mas.cbba.agent;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import mas.cbba.Debug;
import mas.cbba.snapshot.CbbaSnapshot;
import mas.cbba.snapshot.CbgaSnapshot;
import mas.cbba.snapshot.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public CbbaAgent(CbgaAgent cbgaAgent, Parcel j) {
        this(cbgaAgent.getDTO());
        Long bid = cbgaAgent.getX().row(j).values().stream().max(Long::compareTo).get();
        this.y.put(j, bid);
        this.z.put(j, HashBiMap.create(cbgaAgent.getX().row(j)).inverse().get(bid));
    }

    public CbbaAgent(AbstractConsensusAgent k, CbgaSnapshot snapshot, Parcel j) {
        this(k.getDTO());
        Long bid = snapshot.getWinningbids().row(j).values().stream().max(Long::compareTo).get();
        this.y.put(j, bid);
        this.z.put(j, HashBiMap.create(snapshot.getWinningbids().row(j)).inverse().get(bid));
    }

    public void constructBundle() {


//        long currentPenalty = calculateRouteCost(getP());


        Set<Parcel> parcels = this.z.keySet();

        Map<Parcel, PDPModel.ParcelState> states = parcels.stream().collect(Collectors.toMap(p -> p, p -> this.getPDPModel().getParcelState(p)));

        Collection<Parcel> availableParcels = this.getPDPModel().getParcels(PDPModel.ParcelState.ANNOUNCED, PDPModel.ParcelState.AVAILABLE);

        Debug.logParcelListForAgent(this, states, availableParcels);

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
                if(!this.equals(this.z.get(parcel))){
//
//                    for(int pos = 0; pos <= getP().size(); pos++){
//                        //calculate a bid for each position
//                        long bid = calculateRouteCostAtPosition(getP(),parcel,pos) - currentPenalty;
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
//                    LoggerFactory.getLogger(this.getClass()).info("CalculateBestRouteWith value for agent {} of parcel {}: {}", this, parcel, bid);
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


//    @Override
//    public boolean findConsensus() {
//
//        // Send snapshot to all agents
//        // Construct snapshot message
//        //TODO kan ook via this.getCurrentTime(), geeft rechtstreeks long value.
//        boolean hasNewInformation = this.getCommDevice().get().getUnreadCount() > 0;
//
//        sendSnapshot(new CbbaSnapshot(this, this.getCurrentTimeLapse()));
//
//        evaluateMessages();
//
//        return !hasNewInformation;
//    }

    /**
     * Evaluate a single snapshot message from another sender
     */
    public void evaluateSnapshot(Snapshot s, AbstractConsensusAgent sender){
        if(!(s instanceof CbbaSnapshot)){
            throw new IllegalArgumentException("Snapshot does not have the right format. Expected CbbaSnapshot");
        }

        CbbaSnapshot otherSnapshot = (CbbaSnapshot) s;



        // TODO Original Cbba Table for bid evaluation.
//        CbbaSnapshot mySnapshot = (CbbaSnapshot) this.getSnapshot();

        for(Parcel parcel : this.getZ().keySet()){
            //If the incoming snapshot has no information about this parcel, continue to the next one.
            if(!otherSnapshot.getY().containsKey(parcel) && !otherSnapshot.getZ().containsKey(parcel)){
                continue;
            }
            AbstractConsensusAgent myIdea = this.getZ().get(parcel);
            AbstractConsensusAgent otherIdea = otherSnapshot.getZ().get(parcel);

            if(sender.equals(otherIdea)){
                senderThinksHeWins(sender, parcel, myIdea, otherSnapshot);
                continue;
            }
            if(this.equals(otherIdea)){
                senderThinksIWin(sender, parcel, myIdea, otherSnapshot);
                continue;
            }
            if(otherIdea != null && !sender.equals(otherIdea) && !this.equals(otherIdea)){
                senderThinksSomeoneElseWins(sender, parcel, myIdea, otherIdea, otherSnapshot);
                continue;
            }
            if(otherIdea == null){
                senderThinksNododyWins(sender, parcel, myIdea, otherSnapshot);
                continue;
            }


        }

    }

    @Override
    protected Snapshot generateSnapshot() {
        return new CbbaSnapshot(this, this.getCurrentTimeLapse());
    }

    private void senderThinksHeWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot otherSnapshot) {
        //I think I win
        if(this.equals(myIdea)){
            if(compareBids(otherSnapshot.getY().get(parcel),sender,this.getY().get(parcel),this))
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
                    || (compareBids(otherSnapshot.getY().get(parcel),sender,this.getY().get(parcel),myIdea)))
                update(parcel, otherSnapshot);
            return;
        }
        if(myIdea == null){
            update(parcel, otherSnapshot);
            return;
        }

        throw new IllegalArgumentException("Something went wrong in senderThinksHeWins: unreachable code.");
    }

    private void senderThinksIWin(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot otherSnapshot) {
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

    private void senderThinksSomeoneElseWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, AbstractConsensusAgent otherIdea, CbbaSnapshot otherSnapshot) {

        Long otherTimeStamp = otherSnapshot.getCommunicationTimestamps().get(otherIdea);
        Long myTimeStamp = this.getCommunicationTimestamps().get(otherIdea);
        boolean otherHasNewerSnapshotForM = myTimeStamp == null || otherTimeStamp > myTimeStamp;

        if(this.equals(myIdea)) {
            if(otherHasNewerSnapshotForM
                    && (compareBids(otherSnapshot.getY().get(parcel),otherIdea,this.getY().get(parcel),myIdea)))
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
                    && (compareBids(otherSnapshot.getY().get(parcel),otherIdea,this.getY().get(parcel),myIdea)))
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
    private void senderThinksNododyWins(AbstractConsensusAgent sender, Parcel parcel, AbstractConsensusAgent myIdea, CbbaSnapshot otherSnapshot) {
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

        if(bid == null)
            throw new IllegalArgumentException("Bids can't be null.");
        LoggerFactory.getLogger(this.getClass()).info("SetWinningBid {} {} {}", parcel, agent, bid);

        this.y.put(parcel, bid);
        this.z.put(parcel, agent);
    }

    @Override
    protected void handleLostParcels(Parcel cause, List<Parcel> parcels) {
        super.handleLostParcels(cause,parcels);
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
        super.removeParcel(parcel);
        this.y.remove(parcel);
        this.z.remove(parcel);
    }

    public Map<Parcel, Long> getY() {
        return ImmutableMap.copyOf(y);
    }

    public Map<Parcel, AbstractConsensusAgent> getZ() {
        return new HashMap<>(this.z);
    }


}
