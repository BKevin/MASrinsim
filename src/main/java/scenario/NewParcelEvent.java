package scenario;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.util.TimeWindow;
import main.MyParcel;

/**
 * Created by KevinB on 9/05/2016.
 */
public class NewParcelEvent implements TimedEvent {//extends AddParcelEvent

    private final long triggerTime;
    private ParcelDTO parcelDto;

    public NewParcelEvent(long time, Point pickupLocation, Point deliverLocation, int serviceDuration, int neededCapacity, TimeWindow pickup, TimeWindow deliver){
        triggerTime = time;
        parcelDto = Parcel
                .builder(pickupLocation,
                        deliverLocation)
                .serviceDuration(serviceDuration)
                .neededCapacity(neededCapacity)
                .pickupTimeWindow(pickup)
                .deliveryTimeWindow(deliver)
                .buildDTO();
    }


    public long getTime() {
        return triggerTime;
    }

    public ParcelDTO getParcelDTO() {
        return parcelDto;
    }


    public static TimedEventHandler<NewParcelEvent> defaultHandler(){
        return new NewParcelEventHandler();
    }

    private static class NewParcelEventHandler implements TimedEventHandler {
        @Override
        public void handleTimedEvent(TimedEvent timedEvent, SimulatorAPI simulatorAPI) {
            if(timedEvent.getClass() != NewParcelEvent.class)
                throw new IllegalArgumentException("NewParcelEventHandler can only handle NewParcelEvents and not " + timedEvent.getClass().toString());

            NewParcelEvent newParcelEvent = (NewParcelEvent) timedEvent;

            simulatorAPI.register(new MyParcel(
                    newParcelEvent.getParcelDTO()));
        }
    }
}