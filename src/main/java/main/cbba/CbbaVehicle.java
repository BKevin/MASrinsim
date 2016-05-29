package main.cbba;

import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import main.MyVehicle;
import main.cbba.snapshot.Snapshot;

/**
 * Created by pieter on 26.05.16.
 */
public class CbbaVehicle extends MyVehicle implements ConsensusAgent {

    private Snapshot snapshot;

    public CbbaVehicle(VehicleDTO vehicleDTO) {
        super(vehicleDTO);
    }

    @Override
    public void constructBundle() {

    }

    @Override
    public void findConsensus() {

    }

    protected void sendSnapshot(Snapshot snapshot){
        // If the current information is different from the information we sent last time, resend.
        if(!this.getSnapshot().equals(snapshot)){

            this.setSnapshot(snapshot);

            //TODO getVehicles: send to agent k with g_ik(t) = 1.
            for(Vehicle c : this.getPDPModel().getVehicles()) {
                this.getCommDevice().get().send(snapshot, c);
            }
        };
    }

    protected Snapshot getSnapshot() {
        return snapshot;
    }

    protected void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
    }
}
