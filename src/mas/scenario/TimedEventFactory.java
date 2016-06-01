package mas.scenario;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * Created by KevinB on 9/05/2016.
 */
public class TimedEventFactory {


    public static TimedEvent makeTimedEventFromString(String string){
        String[] split = string.split(" ");

        if("NewParcel".equals(split[0])){
            return makeNewParcelEvent(split);
        }
        if("NewMultiParcel".equals(split[0])){
            return makeNewMultiParcelEvent(split);
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
        int maxRequiredAgents = Integer.parseInt(split[5]);

        return new NewVehicleEvent(time, position, capacity, speed, maxRequiredAgents);
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

        String[] twoWindows = split[6].split("=");
        String[] pickupWindow = twoWindows[0].split("/");
        TimeWindow pickup = TimeWindow.create(Long.parseLong(pickupWindow[0]), Long.parseLong(pickupWindow[1]));
        String[] deliverWindow = twoWindows[1].split("/");
        TimeWindow deliver = TimeWindow.create(Long.parseLong(deliverWindow[0]), Long.parseLong(deliverWindow[1]));


        return new NewParcelEvent(time, pickupLocation, depositLocation, serviceDuration, capacity, pickup, deliver );
    }

    private static TimedEvent makeNewMultiParcelEvent(String[] split) {

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

        String[] twoWindows = split[6].split("=");
        String[] pickupWindow = twoWindows[0].split("/");
        TimeWindow pickup = TimeWindow.create(Long.parseLong(pickupWindow[0]), Long.parseLong(pickupWindow[1]));
        String[] deliverWindow = twoWindows[1].split("/");
        TimeWindow deliver = TimeWindow.create(Long.parseLong(deliverWindow[0]), Long.parseLong(deliverWindow[1]));

        int requiredAgents = Integer.parseInt(split[7]);
        return new NewMultiParcelEvent(time, pickupLocation, depositLocation, serviceDuration, capacity, pickup, deliver, requiredAgents);
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
