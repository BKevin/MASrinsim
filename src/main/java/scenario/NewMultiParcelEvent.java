package scenario;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.util.TimeWindow;
import main.MyParcel;
import main.cbba.parcel.MultiParcel;

/**
 * Created by KevinB on 30/05/2016.
 */
public class NewMultiParcelEvent extends NewParcelEvent {

    private final int requiredAgents;

    public NewMultiParcelEvent(long time, Point pickupLocation, Point depositLocation, int serviceDuration, int capacity, TimeWindow pickup, TimeWindow deliver, int requiredAgents) {
        super(time,pickupLocation,depositLocation,serviceDuration,capacity,pickup,deliver);
        this.requiredAgents = requiredAgents;
    }

    public int getRequiredAgents() {
        return requiredAgents;
    }

    public static TimedEventHandler<NewParcelEvent> defaultHandler(){
        return new NewMultiParcelEventHandler();
    }

    private static class NewMultiParcelEventHandler implements TimedEventHandler {
        @Override
        public void handleTimedEvent(TimedEvent timedEvent, SimulatorAPI simulatorAPI) {
            if(timedEvent.getClass() != NewMultiParcelEvent.class)
                throw new IllegalArgumentException("NewParcelEventHandler can only handle NewMultiParcelEvents and not " + timedEvent.getClass().toString());

            NewMultiParcelEvent newMultiParcelEvent = (NewMultiParcelEvent) timedEvent;

            simulatorAPI.register(new MultiParcel(
                    newMultiParcelEvent.getParcelDTO(),newMultiParcelEvent.getRequiredAgents()));
        }
    }
}