package scenario;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import main.MyParcel;

/**
 * Created by KevinB on 9/05/2016.
 */
public class NewParcelEventHandler implements TimedEventHandler {
    @Override
    public void handleTimedEvent(TimedEvent timedEvent, SimulatorAPI simulatorAPI) {
        if(timedEvent.getClass() != NewParcelEvent.class)
            throw new IllegalArgumentException("NewParcelEventHandler can only handle NewParcelEvents and not " + timedEvent.getClass().toString());

        NewParcelEvent newParcelEvent = (NewParcelEvent) timedEvent;

        simulatorAPI.register(new MyParcel(
                    newParcelEvent.getParcelDTO(),
                    simulatorAPI.getRandomGenerator()));
    }
}
