package main.cbba.agent;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.*;
import main.cbba.parcel.MultiParcel;
import main.cbba.snapshot.CbbaSnapshot;
import main.cbba.snapshot.CbgaSnapshot;
import main.cbba.snapshot.Snapshot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by pieter on 26.05.16.
 */
public class CbgaAgent extends CbbaVehicle{

    /* m*n matrix with the winning bids of agents.
     * Xij is equal to the winning bid of agent i for task j or equal to  0 if no assignment has been made.
     */
    private Table<Parcel, ConsensusAgent, Double> winningBids; //sparse table

    public CbgaAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);

        this.winningBids = HashBasedTable.create(
                this.getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE).size(), //expected parcels
                this.getPDPModel().getVehicles().size()); //expected vehicles

    }


    /**
     * @return Immutable version of the winning bids array
     */
    public ImmutableTable<Parcel, ConsensusAgent, Double> getWinningBids() {
        return ImmutableTable.copyOf(winningBids);
    }

    /**
     * Set winning bid value for the given Parcel and ConsensusAgent
     * @param parcel
     * @param agent
     * @param bid
     */
    protected void setWinningBid(Parcel parcel, ConsensusAgent agent, Double bid){
        this.winningBids.put(parcel, agent, bid);
    }


    @Override
    public void constructBundle() {

        // FIXME Use winningbids-table
        super.constructBundle();


        //TODO implement constructbundle
        throw new UnsupportedOperationException();

    }

    /**
     * Evaluate a single snapshot message from another sender
     *
     */
    protected void evaluateSnapshot(Snapshot s, ConsensusAgent k){
        if(!(s instanceof CbgaSnapshot)){
            throw new IllegalArgumentException("Snapshot does not have the right format. Expected CbgaSnapshot");
        }

        CbgaSnapshot snapshot = (CbgaSnapshot) s;

        ImmutableTable<Parcel, ConsensusAgent, Double> bids = snapshot.getWinningbids();

        // Convenience variable to adhere to the original algorithm
        CbgaAgent i = this;

        for(Parcel j : bids.rowKeySet()){

            if(!(j instanceof MultiParcel)) {
                // FIXME link to CBBA table
//                super.evaluateSnapshot(new CbbaSnapshot(null, null));
            }

            else{
//                MultiParcel mj = (MultiParcel) j;

                Map<ConsensusAgent, Long> timestamps = snapshot.getCommunicationTimestamps();


                // (for all m) m /= i
                for(ConsensusAgent m : bids.row(j).keySet()){
                    if(m.equals(i))
                        continue;

                    // Agent i believes an assignment is taking place between agent m and task j
                    //(if) Xijm>0
                    if(i.getWinningBids().get(j, m) > 0) {

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
            for(ConsensusAgent m : bids.row(j).keySet()){
                //(if) m /= i (and) Xijm > 0 (and) Xkjm >= 0
                if(m.equals(i) || i.getWinningBids().get(j, m) == 0)
                    continue;

                // Number of agents assigned to J according to I
                List<Double> bidsOnJ = i.getWinningBids().row(j).values().stream().filter((Double d) -> 0.0 < d).collect(Collectors.<Double>toList());

                // There are less than the required number of agents assigned and m does a bid on j according to k
                // (Sum of all N: Xijn > 0) < Qj
                if(bidsOnJ.size() < mj.getRequiredAgents()){
                    // Assign m to j
                    //Xijm = Xkjm
                    i.setWinningBid(j, m, bids.get(j, m));
                }

                // (Assumes the number of required agents is reached)
                // Determine the minimum bid value and the associated agent N for task J
                Optional<Double> minBid = bidsOnJ.stream().min(Double::compareTo);

                if(!minBid.isPresent() ) {
                    throw new IllegalArgumentException("No minimum bid found in bid table.");
                }
                ConsensusAgent n = HashBiMap.<ConsensusAgent, Double>create(bids.row(j)).inverse().get(minBid.get());

                // If the minimum bid of N is lower than the bid of M for J, assign M instead of N
                // (Min of all n: Xijn) < Xkjm
                if(minBid.get().compareTo(bids.get(j, m)) < 0 ){
                    i.setWinningBid(j, n, 0D);
                    i.setWinningBid(j, m, bids.get(j, m));
                }
                // If the minimum bid of N is equal to the bid of M for J, the greatest ID (hashvalue) wins the assignment to J
                else if(minBid.get().compareTo(bids.get(j, m)) == 0 && i.hashCode() > m.hashCode()){
                    i.setWinningBid(j, n, 0D);
                    i.setWinningBid(j, m, bids.get(j, m));
                }

            }

        }
    }

}
