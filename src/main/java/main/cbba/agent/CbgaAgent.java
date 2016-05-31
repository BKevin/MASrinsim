package main.cbba.agent;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.*;
import main.MyParcel;
import main.cbba.parcel.MultiParcel;
import main.cbba.parcel.SubParcel;
import main.cbba.snapshot.CbbaSnapshot;
import main.cbba.snapshot.CbgaSnapshot;
import main.cbba.snapshot.Snapshot;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by pieter on 26.05.16.
 */
public class CbgaAgent extends AbstractConsensusAgent {

    /* m*n matrix with the winning bids of agents.
     * Xij is equal to the winning bid of agent i for task j or equal to  0 if no assignment has been made.
     */
    private Table<Parcel, AbstractConsensusAgent, Long> X; //sparse table

    public CbgaAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);

        this.X = HashBasedTable.create(
                this.getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE).size(), //expected parcels
                this.getPDPModel().getVehicles().size()); //expected vehicles

    }


    /**
     * @return Immutable version of the winning bids array
     */
    public ImmutableTable<Parcel, AbstractConsensusAgent, Long> getX() {
        return ImmutableTable.copyOf(X);
    }

    @Override
    protected void replaceWinningBid(Parcel parcel, AbstractConsensusAgent from, AbstractConsensusAgent to, Long bid){
        this.X.put(parcel, from, 0L);
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

        this.X.put(parcel, agent, bid);

    }

    @Override
    protected void handleLostParcels(List<Parcel> parcels) {
        // remove bids of this Agent on the given parcels
        this.X.column(this).replaceAll(
                ((parcel, bid)
                        -> parcels.contains(parcel)
                        ? 0L
                        : bid));
    }

    @Override
    protected void addParcel(Parcel parcel) {
        //FIXME moet er iets worden toegevoegd?
    }

//    /**
//     * We handle MultiParcel too here, allocation is different than for Cbba single Parcels
//     * @param parcel
//     * @param agent
//     */
//    @Override
//    protected void allocateParcelToWinner(Parcel parcel, AbstractConsensusAgent agent) {
//        if (parcel instanceof MultiParcel) {
//
//            ((MultiParcel) parcel).allocateTo(agent);
//
//            //Find out who other allocated vehicles are
//        }
//        if (parcel instanceof SubParcel){
//            throw new IllegalArgumentException("Should not directly allocate parcels of type "+parcel.getClass().getName());
//        }
//        else if(parcel instanceof MyParcel){
//            ((MyParcel) parcel).allocateTo(agent);
//        }
//        else{
//            throw new IllegalArgumentException("Should not allocate parcels of type "+parcel.getClass().getName());
//        }
//    }

    @Override
    public void constructBundle() {

        Set<Parcel> parcels = X.column(this).keySet();

        // Get all parcels not already in B
        List<Parcel> notInB = parcels.stream().filter(p -> !this.getB().contains(p)).collect(Collectors.toList());

        boolean bIsChanging = true;

        while(bIsChanging) {

            // Find best route values for every parcel currently not assigned to this vehicle
            Map<Parcel, Long> c_ij =
                    notInB.stream().collect(Collectors.toMap(Function.identity(), this::calculateBestRouteWith));

            // Get the best bid
            Optional<Map.Entry<Parcel, Long>> optBestEntry = c_ij.entrySet().stream()
                    // Remove all entries for which c_ij < max(y_ij)
                    .filter(entry ->
                            this.isBetterBidThan(
                                    // bid value of the entry
                                    entry.getValue(),
                                    // calculate the current maximum bid for every parcel not in B
                                    // TODO could be cached?
                                    this.getX().row(entry.getKey()).values().stream()
                                            .max(Long::compareTo).get()
                            )
                    )
                    // Calculate the minimum argument in h_ij
                    .min(new Comparator<Map.Entry<? extends Parcel, Long>>() {
                        @Override
                        public int compare(Map.Entry<? extends Parcel, Long> parcelLongEntry, Map.Entry<? extends Parcel, Long> t1) {
                            return parcelLongEntry.getValue().compareTo(t1.getValue());
                        }
                    });

            if(bIsChanging = optBestEntry.isPresent()) {
                Map.Entry<? extends Parcel, Long> bestEntry = optBestEntry.get();

                this.getB().add(bestEntry.getKey());
                this.getP().add(this.calculateBestRouteIndexWith(bestEntry.getKey()), bestEntry.getKey());

                this.setWinningBid(bestEntry.getKey(), this, bestEntry.getValue());
            }
        }

    }

    @Override
    public void findConsensus() {

        // Send snapshot to all agents
        // Construct snapshot message
        //TODO kan ook via this.getCurrentTime(), geeft rechtstreeks long value.
        sendSnapshot(new CbgaSnapshot(this, this.getCurrentTimeLapse()));

        evaluateMessages();
    }

    /**
     * Evaluate a single snapshot message from another sender
     *
     */
    public void evaluateSnapshot(Snapshot s, AbstractConsensusAgent k){
        if(!(s instanceof CbgaSnapshot)){
            throw new IllegalArgumentException("Snapshot does not have the right format. Expected CbgaSnapshot");
        }

        CbgaSnapshot snapshot = (CbgaSnapshot) s;

        ImmutableTable<Parcel, AbstractConsensusAgent, Long> bids = snapshot.getWinningbids();

        // Convenience variable to adhere to the original algorithm
        CbgaAgent i = this;

        for(Parcel j : bids.rowKeySet()){

            if(!(j instanceof MultiParcel)) {
                // FIXME link to CBBA table method
            }

            else{
//                MultiParcel mj = (MultiParcel) j;

                Map<AbstractConsensusAgent, Long> timestamps = snapshot.getCommunicationTimestamps();


                // (for all m) m /= i
                for(AbstractConsensusAgent m : bids.row(j).keySet()){
                    if(m.equals(i))
                        continue;

                    // Agent i believes an assignment is taking place between agent m and task j
                    //(if) Xijm>0
                    if(i.getX().get(j, m) > 0) {

                        // If K has newer information about assignment of task j to  M, update info.
                        //(if) Skm > Sim (or) m = k
                        if (timestamps.get(m) > i.getCommunicationTimestamps().get(m) || m.equals(k)) {

                            // Xijm = Xkjm
                            i.setWinningBid(j, m, bids.get(j, m));
                        }
                    }
                }
            }

            MultiParcel mj = (MultiParcel) j;

            // (for all m E A)
            for(AbstractConsensusAgent m : bids.row(j).keySet()){
                //(if) m /= i (and) Xijm > 0 (and) Xkjm >= 0
                if(m.equals(i) || i.getX().get(j, m) == 0)
                    continue;

                // Number of agents assigned to J according to I
                List<Long> bidsOnJ = i.getX().row(j).values().stream().filter((Long d) -> 0L < d).collect(Collectors.<Long>toList());

                // There are less than the required number of agents assigned and m does a bid on j according to k
                // (Sum of all N: Xijn > 0) < Qj
                if(bidsOnJ.size() < mj.getRequiredAgents()){
                    // Assign m to j
                    //Xijm = Xkjm
                    i.setWinningBid(mj, m, bids.get(mj, m));
                }

                // (Assumes the number of required agents is reached)
                // Determine the maximum bid value and the associated agent N for task J
                Optional<Long> minBid = bidsOnJ.stream().max(Long::compareTo);

                if(!minBid.isPresent() ) {
                    throw new IllegalArgumentException("No minimum bid found in bid table.");
                }
                AbstractConsensusAgent n = HashBiMap.<AbstractConsensusAgent, Long>create(bids.row(j)).inverse().get(minBid.get());

                // If the maximum bid of N is higher than the bid of M for J, assign M instead of N
                // (Min of all n: Xijn) < Xkjm
                if(this.isBetterBidThan(minBid.get(), bids.get(j, m))){
                    i.replaceWinningBid(j, n, m, bids.get(j, m));
                }
                // If the maximum bid of N is equal to the bid of M for J, the greatest ID (hashvalue) wins the assignment of J
                else if(minBid.get().compareTo(bids.get(j, m)) == 0 && i.hashCode() > m.hashCode()){
                    i.replaceWinningBid(j, n, m, bids.get(j, m));
                }

            }

        }
    }

}
