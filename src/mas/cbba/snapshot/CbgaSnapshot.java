package mas.cbba.snapshot;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableTable;
import mas.cbba.agent.AbstractConsensusAgent;
import mas.cbba.agent.CbgaAgent;

import java.util.Optional;

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
                    && sn.getWinningbids().values().containsAll(this.getWinningbids().values());

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

    @Override
    public AbstractConsensusAgent getWinningAgentBy(Parcel parcel) {

        Long minBid = getWinningBidBy(parcel);

        for(AbstractConsensusAgent a : this.getWinningbids().row(parcel).keySet()){
            if(this.getWinningbids().get(parcel, a).equals(minBid)){
                return a;
            }
        }
        return null;
    }

    @Override
    public Long getWinningBidBy(Parcel parcel) {
        Optional<Long> bid = this.getWinningbids().row(parcel).values().stream().min(Long::compareTo);
        return bid.isPresent() ? bid.get() : Long.MAX_VALUE;
    }
}
