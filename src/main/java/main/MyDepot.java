package main;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * Created by KevinB on 8/05/2016.
 */
public class MyDepot extends Depot implements CommUser, TickListener{

    private Optional<CommDevice> device;

    public MyDepot(Point position, int depotCapacity) {
        super(position);
        setCapacity(depotCapacity);
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(this.getRoadModel().getPosition(this));
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {

        device = Optional.of(commDeviceBuilder.build());
    }

    @Override
    public void tick(TimeLapse timeLapse) {
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }
}
