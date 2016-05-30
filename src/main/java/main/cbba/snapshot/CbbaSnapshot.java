package main.cbba.snapshot;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import main.cbba.agent.CbbaAgent;
import main.cbba.agent.AbstractConsensusAgent;

import java.util.Map;

/**
 * Created by pieter on 26.05.16.
 */
public class CbbaSnapshot extends Snapshot {

    private final Map<Parcel, Long> y;
    private final Map<Parcel, AbstractConsensusAgent> z;

    public CbbaSnapshot(CbbaAgent agent, TimeLapse time) {
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

}
