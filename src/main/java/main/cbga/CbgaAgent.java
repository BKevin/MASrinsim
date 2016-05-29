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
         * Xij is equal to the winning bid of agent i for task j or equal to
    0 if no assignment has been made.
         */
    private Table<Parcel, ConsensusAgent, Double> winningBids; //sparse table

    private Map<ConsensusAgent, Long> communicationTimestamps;

    public CbgaAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);

        this.winningBids = HashBasedTable.create(
                this.getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE).size(), //expected parcels
                this.getPDPModel().getVehicles().size()); //expected vehicles

        this.communicationTimestamps = new HashMap<>();
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

    public ImmutableMap<ConsensusAgent, Long> getCommunicationTimestamps() {
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

        super.constructBundle();

        //TODO implement constructbundle
        throw new UnsupportedOperationException();

    }

    @Override
    public void findConsensus() {
        //TODO move to superclass

        // Send snapshot to all agents
        // Construct snapshot message
        //TODO kan ook via this.getCurrentTime(), geeft rechtstreeks long value.
        sendSnapshot(new CbgaSnapshot(this, this.getCurrentTimeLapse()));

        //receive from all agents
        evaluateMessages();

    }

    /**
     * Override
     */
    protected void evaluateMessages() {

        for(Message message : this.getCommDevice().get().getUnreadMessages()){
            //if AuctionedParcelMessage then calculate bid and send BidMessage
            final MessageContents contents = message.getContents();

            CommUser sender = message.getSender();

            if(contents instanceof CbgaSnapshot){

                this.setCommunicationTimestamp(message);

                CbgaSnapshot snapshot = (CbgaSnapshot) contents;

                ImmutableTable<Parcel, ConsensusAgent, Double> bids = snapshot.getWinningbids();

                for(ConsensusAgent c : bids.columnKeySet()){

                    for(bids.column(c).keySet()

                }

                // TODO consensus from CBBA
                // Change signature accomodate fast hook into CBBA consensus algorithm
                super.findConsensus();


            }

    }

//    @Override
//    protected void sendSnapshot(Snapshot snapshot) {
//
//
//        CbgaSnapshot snapshot = );
//
//        super.sendSnapshot(snapshot);
//
//    }
}
