package mas.cbba.snapshot;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableTable;
import mas.cbba.agent.AbstractConsensusAgent;
import mas.cbba.agent.CbgaAgent;

/**
 * Snapshot of an CbgaAgent
 */
public class CbgaSnapshot extends Snapshot {

    private final ImmutableTable<Parcel, AbstractConsensusAgent, Long> winningbids;

    public CbgaSnapshot(CbgaAgent agent, Long time) {

        super(agent, time);

        this.winningbids = agent.getX();
    }


    @Override
    public boolean equals(Object o) {
        boolean result;
        if(result = (super.equals(o) && o instanceof CbgaSnapshot)){
            CbgaSnapshot sn = (CbgaSnapshot) o;

            result = this.getWinningbids().values().containsAll((sn.getWinningbids().values()))
                    && this.getWinningbids().rowKeySet().containsAll(sn.getWinningbids().rowKeySet())
                    && this.getWinningbids().columnKeySet().containsAll(sn.getWinningbids().columnKeySet());
        }
        return result ;
    }

    public ImmutableTable<Parcel, AbstractConsensusAgent, Long> getWinningbids() {
        return winningbids;
    }

    @Override
    public String toString() {
        return super.toString() + "\nx: " + getWinningbids();
    }
}
