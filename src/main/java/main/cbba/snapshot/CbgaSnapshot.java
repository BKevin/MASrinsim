package main.cbba.snapshot;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import main.cbba.ConsensusAgent;
import main.cbga.CbgaAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Snapshot of an CbgaAgent
 */
public class CbgaSnapshot extends Snapshot {


    private final ImmutableTable<Parcel, ConsensusAgent, Double> winningbids;

    private final ImmutableMap<ConsensusAgent, Long> timestamps;

    public CbgaSnapshot(CbgaAgent agent, TimeLapse time) {

        super(agent, time);

        this.winningbids = agent.getWinningBids();

        this.timestamps = agent.getCommunicationTimestamps();
    }


    @Override
    public boolean equals(Object o) {
        //TODO implement
        return super.equals(o);
    }

    public ImmutableMap<ConsensusAgent, Long> getTimestamps() {
        return timestamps;
    }

    public ImmutableTable<Parcel, ConsensusAgent, Double> getWinningbids() {
        return winningbids;
    }
}
