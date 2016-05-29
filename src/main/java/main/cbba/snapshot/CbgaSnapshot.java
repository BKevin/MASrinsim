package main.cbba.snapshot;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.collect.ImmutableTable;
import main.cbba.agent.ConsensusAgent;
import main.cbba.agent.CbgaAgent;

/**
 * Snapshot of an CbgaAgent
 */
public class CbgaSnapshot extends Snapshot {

    private final ImmutableTable<Parcel, ConsensusAgent, Double> winningbids;

    public CbgaSnapshot(CbgaAgent agent, TimeLapse time) {

        super(agent, time);

        this.winningbids = agent.getWinningBids();
    }


    @Override
    public boolean equals(Object o) {
        //TODO implement
        return super.equals(o);
    }

    public ImmutableTable<Parcel, ConsensusAgent, Double> getWinningbids() {
        return winningbids;
    }

}
