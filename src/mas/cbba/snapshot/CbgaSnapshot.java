package mas.cbba.snapshot;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.collect.ImmutableTable;
import mas.cbba.agent.AbstractConsensusAgent;
import mas.cbba.agent.CbgaAgent;

/**
 * Snapshot of an CbgaAgent
 */
public class CbgaSnapshot extends Snapshot {

    private final ImmutableTable<Parcel, AbstractConsensusAgent, Long> winningbids;

    public CbgaSnapshot(CbgaAgent agent, TimeLapse time) {

        super(agent, time);

        this.winningbids = agent.getX();
    }


    @Override
    public boolean equals(Object o) {
        //TODO implement
        return super.equals(o);
    }

    public ImmutableTable<Parcel, AbstractConsensusAgent, Long> getWinningbids() {
        return winningbids;
    }

}
