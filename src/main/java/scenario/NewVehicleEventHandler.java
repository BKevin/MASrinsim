package scenario;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.util.TimeWindow;
import main.MyVehicle;

/**
 * Created by KevinB on 9/05/2016.
 */
public class NewVehicleEventHandler implements TimedEventHandler {

    @Override
    public void handleTimedEvent(TimedEvent timedEvent, SimulatorAPI simulatorAPI) {
        if(timedEvent.getClass() != NewVehicleEvent.class)
            throw new IllegalArgumentException("NewVehicleEventHandler can only handle NewVehicleEvents and not " + timedEvent.getClass().toString());

        NewVehicleEvent newVehicleEvent = (NewVehicleEvent) timedEvent;

        simulatorAPI.register(new MyVehicle(
                        VehicleDTO.builder()
                                .startPosition(newVehicleEvent.getStartLocation())
                                .capacity(newVehicleEvent.getCapacity())
                                .speed(newVehicleEvent.getSpeed())
                                .build()));

    }
}
