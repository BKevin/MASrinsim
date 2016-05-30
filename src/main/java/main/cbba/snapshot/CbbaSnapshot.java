package main.cbba.snapshot;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import main.cbba.agent.CbbaVehicle;
import main.cbba.agent.ConsensusAgent;

import java.util.Map;

/**
 * Created by pieter on 26.05.16.
 */
public class CbbaSnapshot extends Snapshot {

    private final Map<Parcel, Long> y;
    private final Map<Parcel, ConsensusAgent> z;

    public CbbaSnapshot(CbbaVehicle agent, TimeLapse time) {
        super(agent, time);

        this.y = agent.getY();
        this.z = agent.getZ();

    }

    public Map<Parcel, Long> getY() {
        return y;
    }

    public Map<Parcel, ConsensusAgent> getZ() {
        return z;
    }

}
