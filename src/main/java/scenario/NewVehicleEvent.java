package scenario;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import main.MyVehicle;

/**
 * Created by KevinB on 9/05/2016.
 */
public class NewVehicleEvent implements TimedEvent {

    private final long triggerTime;
    private final Point startLocation;
    private final int capacity;
    private final double speed;

    public NewVehicleEvent(long time, Point newStartLocation, int newCapacity, double newSpeed){
        triggerTime = time;
        startLocation = newStartLocation;
        capacity = newCapacity;
        speed = newSpeed;
    }

    @Override
    public long getTime() {
        return triggerTime;
    }

    public Point getStartLocation() {
        return startLocation;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getSpeed() {
        return speed;
    }



    public static TimedEventHandler<NewVehicleEvent> defaultHandler(){
        return new NewParcelEventHandler();
    }
    private static class NewParcelEventHandler implements TimedEventHandler {

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
}
