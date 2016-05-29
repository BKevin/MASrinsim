package main.cbba.snapshot;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import main.MyParcel;
import main.cbba.agent.CbbaVehicle;
import main.cbba.agent.ConsensusAgent;

import java.util.Map;

/**
 * Created by pieter on 26.05.16.
 */
public class CbbaSnapshot extends Snapshot {

    private final Map<MyParcel, Long> y;
    private final Map<MyParcel, ConsensusAgent> z;

    public CbbaSnapshot(CbbaVehicle agent, TimeLapse time) {
        super(agent, time);

        this.y = agent.getY();
        this.z = agent.getZ();

    }

    public Map<MyParcel, Long> getY() {
        return y;
    }

    public Map<MyParcel, ConsensusAgent> getZ() {
        return z;
    }

}
