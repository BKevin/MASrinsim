package scenario;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;

/**
 * Created by KevinB on 9/05/2016.
 */
public class TimedEventFactory {


    public static TimedEvent makeTimedEventFromString(String string){
        String[] split = string.split(" ");

        if("NewParcel".equals(split[0])){
            return makeNewParcelEvent(split);
        }
        if("NewVehicle".equals(split[0])){
            return makeNewVehicleEvent(split);
        }
        if("NewDepot".equals(split[0])){
            return makeNewDepotEvent(split);
        }
        if("info".equals(split[0])){
            return null;
        }

        throw new IllegalArgumentException("Unidentified TimedEvent");
    }

    private static TimedEvent makeNewVehicleEvent(String[] split) {
        long time = Long.parseLong(split[1]);
        Point position;
        String[] co = split[2].split(",");
        double x = Double.parseDouble(co[0]);
        double y = Double.parseDouble(co[1]);
        position = new Point(x,y);
        int capacity = Integer.parseInt(split[3]);
        double speed = Double.parseDouble(split[4]);

        return new NewVehicleEvent(time, position, capacity, speed);
    }

    private static TimedEvent makeNewParcelEvent(String[] split) {

        long time = Long.parseLong(split[1]);
        Point pickupLocation;
        String[] co1 = split[2].split(",");
        double x1 = Double.parseDouble(co1[0]);
        double y1 = Double.parseDouble(co1[1]);
        pickupLocation = new Point(x1,y1);
        Point depositLocation;
        String[] co2 = split[3].split(",");
        double x2 = Double.parseDouble(co2[0]);
        double y2 = Double.parseDouble(co2[1]);
        depositLocation = new Point(x2,y2);
        int serviceDuration = Integer.parseInt(split[4]);
        int capacity = Integer.parseInt(split[5]);

        return new NewParcelEvent(time, pickupLocation, depositLocation, serviceDuration, capacity);
    }

    private static TimedEvent makeNewDepotEvent(String[] split) {
        long time = Long.parseLong(split[1]);
        Point position;
        String[] co = split[2].split(",");
        double x = Double.parseDouble(co[0]);
        double y = Double.parseDouble(co[1]);
        position = new Point(x,y);
        int capacity = Integer.parseInt(split[3]);

        return new NewDepotEvent(time, position, capacity);
    }


}
