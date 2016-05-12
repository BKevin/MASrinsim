package scenario;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;

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
}
