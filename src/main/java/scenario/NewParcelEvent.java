package scenario;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;

/**
 * Created by KevinB on 9/05/2016.
 */
public class NewParcelEvent implements TimedEvent {//extends AddParcelEvent

    private final long triggerTime;
    private ParcelDTO parcelDto;

    public NewParcelEvent(long time, Point pickupLocation, Point deliverLocation, int serviceDuration, int neededCapacity){
        triggerTime = time;
        parcelDto = Parcel
                .builder(pickupLocation,
                        deliverLocation)
                .serviceDuration(serviceDuration)
                .neededCapacity(neededCapacity)
                .buildDTO();
    }


    public long getTime() {
        return triggerTime;
    }

    public ParcelDTO getParcelDTO() {
        return parcelDto;
    }
}