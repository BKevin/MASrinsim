package mas.scenario;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.MyDepot;

import java.io.Serializable;

/**
 * Created by KevinB on 12/05/2016.
 */
public class NewDepotEvent  implements TimedEvent {//extends AddParcelEvent

    private final long triggerTime;
    private final Point depotLocation;
    private final int depotCapacity;

    public NewDepotEvent(long time, Point location, int capacity){
        triggerTime = time;
        depotLocation = location;
        depotCapacity = capacity;
    }


    public long getTime() {
        return triggerTime;
    }

    public Point getDepotLocation() {
        return depotLocation;
    }

    public int getDepotCapacity() {
        return depotCapacity;
    }

    public static TimedEventHandler<NewDepotEvent> defaultHandler(){
        return new NewDepotEventHandler();
    }

    private static class NewDepotEventHandler implements  TimedEventHandler, Serializable{

        @Override
        public void handleTimedEvent(TimedEvent event, SimulatorAPI simulator) {
            if(event.getClass() != NewDepotEvent.class)
                throw new IllegalArgumentException("NewDepotEventHandler can only handle NewDepotEvents and not " + event.getClass().toString());
            NewDepotEvent newDepotEvent = (NewDepotEvent) event;

            simulator.register(new MyDepot(
                    newDepotEvent.getDepotLocation(),
                    newDepotEvent.getDepotCapacity()));
        }
    }
}
