package main.cbga;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import main.cbba.CbbaVehicle;
import main.cbba.ConsensusAgent;
import main.cbba.snapshot.CbbaSnapshot;
import main.cbba.snapshot.CbgaSnapshot;
import main.cbba.snapshot.Snapshot;
import main.comm.AuctionedParcelMessage;
import main.comm.BidMessage;

import java.util.HashMap;
import java.util.Map;

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

        // FIXME Use winningbidstable
        super.constructBundle();


        //TODO implement constructbundle
        throw new UnsupportedOperationException();

    }

    /**
     * Evaluate a single snapshot message from another sender
     *
     */
    protected void evaluateSnapshot(Snapshot s){
        if(!(s instanceof CbgaSnapshot)){
            throw new IllegalArgumentException("Snapshot does not have the right format. Expected CbgaSnapshot");
        }

        CbgaSnapshot snapshot = (CbgaSnapshot) s;

        ImmutableTable<Parcel, ConsensusAgent, Double> bids = snapshot.getWinningbids();

        for(ConsensusAgent c : bids.columnKeySet()){

            for(Parcel p : bids.column(c).keySet()){

                //FIXME repackage snapshot for super evaluation
                super.evaluateSnapshot(null);

            }

        }

        // TODO consensus from CBBA
        // Change signature accomodate fast hook into CBBA consensus algorithm

    }

}
