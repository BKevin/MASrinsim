package main.cbba.snapshot;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import main.cbba.CbbaVehicle;

/**
 * Created by pieter on 26.05.16.
 */
public class CbbaSnapshot extends Snapshot {


    public CbbaSnapshot(CbbaVehicle agent, TimeLapse time) {
        super(agent, time);
    }
}
