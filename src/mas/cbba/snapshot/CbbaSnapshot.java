package mas.cbba.snapshot;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import mas.cbba.agent.CbbaAgent;
import mas.cbba.agent.AbstractConsensusAgent;

import java.util.Map;

/**
 * Created by pieter on 26.05.16.
 */
public class CbbaSnapshot extends Snapshot {

    private final Map<Parcel, Long> y;
    private final Map<Parcel, AbstractConsensusAgent> z;

    public CbbaSnapshot(CbbaAgent agent, Long time) {
        super(agent, time);

        this.y = agent.getY();
        this.z = agent.getZ();
    }

    public Map<Parcel, Long> getY() {
        return y;
    }

    public Map<Parcel, AbstractConsensusAgent> getZ() {
        return z;
    }

    @Override
    public String toString() {
        return super.toString() + " \ny= " + y + " \nz= "+ z ;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if(result = (super.equals(o) && o instanceof CbbaSnapshot)){
            CbbaSnapshot s = (CbbaSnapshot) o;

            result = s.getY().entrySet().containsAll(this.getY().entrySet())
                    && s.getZ().entrySet().containsAll(this.getZ().entrySet());
        };

        return result;
    }
}
