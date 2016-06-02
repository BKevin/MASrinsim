package mas.cbba.agent;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.ImmutableMap;
import mas.MyParcel;
import mas.cbba.snapshot.CbbaSnapshot;
import mas.cbba.snapshot.CbgaSnapshot;
import mas.cbba.snapshot.Snapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

//    /**
//     * Create single Parcel CbbaAgent based on CbgaAgent.
//     * @param k Agent reference
//     * @param snapshot Snapshot of CbgaAgent
//     * @param j Parcel for which to create CbbaAgent
//     */
//    public CbbaAgent(AbstractConsensusAgent k, CbgaSnapshot snapshot, Parcel j) {
//        this(k.getDTO());
//        Long bid = snapshot.getWinningbids().row(j).values()
//                .stream()
////                .filter(v -> v.equals(CbgaAgent.NO_BID))
//                .min(Long::compareTo).get();
//        this.y.put(j, bid);
//        // Inverse will fail on the off chance that two agents bid the same:
//        // IllegalArgumentException: value already present
//        for(final AbstractConsensusAgent a : snapshot.getWinningbids().row(j).keySet()){
//            if(bid.equals(snapshot.getWinningbids().row(j).get(a))){
//                if(a == k){
//                    this.z.put(j, this);
//                }
//                else{
//                    this.z.put(j, a);
//                }
//            }
//        }
//
//                snapshot.getWinningbids().row(j).entrySet()
//                        .stream()
//                        .filter((Map.Entry<AbstractConsensusAgent, Long> e) -> !e.getValue().equals(CbgaAgent.NO_BID))
//                        .collect(Collectors.toMap((Map.Entry e) -> e.getKey(), (Map.Entry e) -> e.getValue())))
//                );
//    }

    public void constructBundle() {
        Set<Parcel> parcels = this.z.keySet();

        // Debugging
//        Map<Parcel, PDPModel.ParcelState> states = parcels.stream().collect(Collectors.toMap(p -> p, p -> this.getPDPModel().getParcelState(p)));
//        Collection<Parcel> availableParcels = this.getPDPModel().getParcels(PDPModel.ParcelState.ANNOUNCED, PDPModel.ParcelState.AVAILABLE);
//        Debug.logParcelListForAgent(this, states, availableParcels);


        // Get all parcels not already in B
        // And remove unavailable parcels
        List<Parcel> notInBAndAvailable = parcels.stream()
                .filter(p -> !this.getB().contains(p) && ((MyParcel) p).isAvailable())
                .collect(Collectors.toList());

        boolean bIsChanging = true;
        while(bIsChanging){
            bIsChanging = false;

            long bestBid = NO_BID;
            Parcel bestParcel = null;
//            int bestPosition = -1;
            //look at all parcels
            for(Parcel parcel : notInBAndAvailable){
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

    @Override
    protected Snapshot generateSnapshot() {
        return new CbbaSnapshot(this, this.getCurrentTime());
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

        this.updateBidValue(parcel, agent, bid);

//        LoggerFactory.getLogger(this.getClass()).info("SetWinningBid {} {} {}", parcel, agent, bid);

    }

    @Override
    public void updateBidValue(Parcel j, AbstractConsensusAgent m, Long aLong) {
        this.y.put(j, aLong);
        this.z.put(j, m);
    }

    // FIXME kijk na of dit juist gebeurd door setWinningBid en updateBidValue
//    @Override
//    protected void handleLostParcels(Parcel cause, List<Parcel> parcels) {
//        super.handleLostParcels(cause,parcels);
//        //remove winning bids on the given parcels
//        this.y.replaceAll(
//                ((parcel, bid)
//                        -> parcels.contains(parcel)
//                        ? NO_BID
//                        : bid));
//        //remove winners of the given parcels
//        this.z.replaceAll(
//                ((parcel, winner)
//                        -> parcels.contains(parcel)
//                        ? null
//                        : winner));
//    }

    public Map<Parcel, Long> getY() {
        return ImmutableMap.copyOf(y);
    }

    public Map<Parcel, AbstractConsensusAgent> getZ() {
        return new HashMap<>(this.z);
    }

    @Override
    public Set<Parcel> getParcels() {
        return this.getZ().keySet();
    }

    @Override
    protected AbstractConsensusAgent getWinningAgentBy(Parcel parcel) {
        return this.getZ().get(parcel);
    }

    @Override
    protected Long getWinningBidBy(Parcel parcel) {
        return this.getY().get(parcel);
    }

    /**
     * Add the new parcel to the lists of bids and winners
     * @param parcel
     */
    @Override
    protected void addParcelToBidList(Parcel parcel) {
        this.y.put(parcel,NO_BID);
        this.z.put(parcel,null);
    }

    protected void removeParcelFromBidList(Parcel parcel){
        super.removeParcelFromBidList(parcel);

        this.y.remove(parcel);
        this.z.remove(parcel);
    }


}
