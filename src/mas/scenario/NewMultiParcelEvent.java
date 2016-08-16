package mas.scenario;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.util.TimeWindow;
import mas.MyParcel;
import mas.cbba.parcel.MultiParcel;

import java.io.Serializable;

/**
 * Created by KevinB on 30/05/2016.
 */
public class NewMultiParcelEvent implements TimedEvent {

    private final int requiredAgents;
    private final long triggerTime;

    private ParcelDTO parcelDto;

    public NewMultiParcelEvent(long time, Point pickupLocation, Point deliverLocation, int serviceDuration, int neededCapacity, TimeWindow pickup, TimeWindow deliver, int requiredAgents) {
        triggerTime = time;
        parcelDto = Parcel
                .builder(pickupLocation,
                        deliverLocation)
                .serviceDuration(serviceDuration)
                .neededCapacity(neededCapacity)
                .pickupTimeWindow(pickup)
                .deliveryTimeWindow(deliver)
                .buildDTO();
        this.requiredAgents = requiredAgents;
    }

    public int getRequiredAgents() {
        return requiredAgents;
    }

    public ParcelDTO getParcelDTO() {
        return parcelDto;
    }
    @Override
    public long getTime() {
        return triggerTime;
    }

    public static TimedEventHandler defaultHandler(){
        return new NewMultiParcelEventHandler();
    }
    public static TimedEventHandler separateHandler(){
        return new SeparateMultiParcelEventHandler();
    }


    private static class NewMultiParcelEventHandler implements TimedEventHandler, Serializable {
        @Override
        public void handleTimedEvent(TimedEvent timedEvent, SimulatorAPI simulatorAPI) {
            if(timedEvent.getClass() != NewMultiParcelEvent.class)
                throw new IllegalArgumentException("NewParcelEventHandler can only handle NewMultiParcelEvents and not " + timedEvent.getClass().toString());

            NewMultiParcelEvent newMultiParcelEvent = (NewMultiParcelEvent) timedEvent;

            MultiParcel parcel = new MultiParcel(
                    newMultiParcelEvent.getParcelDTO(), newMultiParcelEvent.getRequiredAgents());
            simulatorAPI.register(parcel);

            for(Parcel p : parcel.getSubParcels()){
                simulatorAPI.register(p);
            }

        }
    }

    private static class SeparateMultiParcelEventHandler2 implements TimedEventHandler, Serializable {
        @Override
        public void handleTimedEvent(TimedEvent timedEvent, SimulatorAPI simulatorAPI) {
            if(timedEvent.getClass() != NewMultiParcelEvent.class)
                throw new IllegalArgumentException("NewParcelEventHandler can only handle NewMultiParcelEvents and not " + timedEvent.getClass().toString());

            NewMultiParcelEvent newMultiParcelEvent = (NewMultiParcelEvent) timedEvent;

            for(int i = 0; i < newMultiParcelEvent.getRequiredAgents(); i++){
                simulatorAPI.register(new MyParcel(
                        newMultiParcelEvent.getParcelDTO()));
            }

        }
    }
    private static class SeparateMultiParcelEventHandler implements TimedEventHandler, Serializable {
        @Override
        public void handleTimedEvent(TimedEvent timedEvent, SimulatorAPI simulatorAPI) {
            if(timedEvent.getClass() != NewMultiParcelEvent.class)
                throw new IllegalArgumentException("NewParcelEventHandler can only handle NewMultiParcelEvents and not " + timedEvent.getClass().toString());

            NewMultiParcelEvent newMultiParcelEvent = (NewMultiParcelEvent) timedEvent;

            simulatorAPI.register(new MultiAdder(newMultiParcelEvent.getRequiredAgents(),newMultiParcelEvent.getParcelDTO(),simulatorAPI));

        }

        private class MultiAdder implements  TickListener{

            private final SimulatorAPI sim;
            private final ParcelDTO parcelDTO;
            private int amountParcels;

            public MultiAdder(int amount, ParcelDTO parcelDTO, SimulatorAPI simulatorApi){
                this.amountParcels = amount;
                this.sim = simulatorApi;
                this.parcelDTO = parcelDTO;
            }

            @Override
            public void tick(TimeLapse timeLapse) {
                if(amountParcels > 0) {
                    sim.register(new MyParcel(parcelDTO));
                    amountParcels -= 1;
                }
            }

            @Override
            public void afterTick(TimeLapse timeLapse) {

            }
        }
    }

}
