package mas.scenario;

import com.github.rinde.rinsim.geom.Point;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by KevinB on 12/05/2016.
 */
public class ScenarioGenerator {
    private static Random rng = new Random();
    private static long currentTime;

    private static double width = 10;
    private static double height = 10;

    private static int amountVehiclesAtStart = 4;
    private static int amountParcelsAtStart = 3;
    private static int amountNewVehicles = 0;
    private static int amountNewParcels = 100;


    private static long vehicleAverageInterArrivalTime; //ms
    private static long vehicleInterArrivalTimeVariation; //ms
    private static int vehicleCapacity = 1;
    private static int vehicleSpeed = 100;


    private static long parcelAverageInterArrivalTime = 50000; //ms
    private static long parcelInterArrivalTimeVariation = 10000; //ms
    private static long parcelAverageTimeWindowVariation = 10000; //ms
    private static int parcelServiceDuration = 1000;
    private static int parcelCapacity = 1;
    private static int maxAmountOfRequiredAgents = 1;
    private static double[] distribution = {1,0};

    private static double expectedTravelTime = 20000;
    private static double travelTimeVariation = 5000;

    public static void main(String[] args) {
        generateScenario();
    }

    private static void generateScenario() {
        currentTime = 0;

        File file = Paths.get("resources/new_scenarios/scene_"+new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date())+".txt").toFile();
        try (BufferedWriter br = new BufferedWriter(
                                            new OutputStreamWriter(
                                                    new FileOutputStream(file), "utf-8"))) {

            String depotEvent = makeNewDepotEventAtStart();
            br.write(depotEvent);
            br.newLine();

            for(int i = 0; i < amountVehiclesAtStart; i++){
                String vehicleEvent = makeVehicleEventAtStart();
                br.write(vehicleEvent);
                br.newLine();
            }
            for(int i = 0; i < amountParcelsAtStart; i++){
                String parcelEvent = makeParcelEventAtStart();
                br.write(parcelEvent);
                br.newLine();
            }

            for(int i = 0; i < amountNewVehicles; i++){
                String vehicleEvent = makeNewVehicleEvent();
                br.write(vehicleEvent);
                br.newLine();
            }
            for(int i = 0; i < amountNewParcels; i++){
                String parcelEvent = makeNewParcelEvent();
                br.write(parcelEvent);
                br.newLine();
            }



            br.close();

            LoggerFactory.getLogger(ScenarioGenerator.class).info("Wrote scenario to {} - {}", file.getCanonicalPath(), file.canWrite());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String makeNewDepotEventAtStart()  {
        return depotEventAtTime(-1);
    }

    private static String depotEventAtTime(long time) {
        return "NewDepot "
                + time + " "
                + height/2 + "," + width /2 + " "
                + amountVehiclesAtStart;
    }

    private static String makeVehicleEventAtStart() {
        return vehicleEventAtTime(-1);
    }

    private static String makeNewVehicleEvent() {
        long rngTime = (long) (currentTime + vehicleAverageInterArrivalTime  + ((2*rng.nextDouble() - 1) * vehicleInterArrivalTimeVariation));
        currentTime = rngTime;
        return vehicleEventAtTime(rngTime);
    }

    private static String vehicleEventAtTime(long time) {
        Point location = randomPoint();
        return "NewVehicle "
                + time + " "
                + location.x + "," + location.y + " "
                + vehicleCapacity + " "
                + vehicleSpeed + " "
                + maxAmountOfRequiredAgents;
    }

    private static String makeParcelEventAtStart() {
        int requiredAgents = getRequiredAgents();
        if (requiredAgents == 1)
            return "NewParcel "
                    + parcelEventAtTime(-1);
        else
            return "NewMultiParcel "
                    + parcelEventAtTime(-1)
                    + requiredAgents;
    }

    private static String makeNewParcelEvent() {
        int requiredAgents = getRequiredAgents();
        long rngTime = (long) (currentTime + parcelAverageInterArrivalTime  + ((2*rng.nextDouble() - 1) * parcelInterArrivalTimeVariation));
        currentTime = rngTime;
        if (requiredAgents == 1)
            return "NewParcel "
                    + parcelEventAtTime(rngTime);
        else
            return "NewMultiParcel "
                    + parcelEventAtTime(rngTime)
                    + requiredAgents;
    }

    private static String parcelEventAtTime(long time) {
        Point location1 = randomPoint();
        Point location2 = randomPoint();
        String timeWindow = makeTimeWindows(time, location1, location2);
        return time + " "
                + location1.x + "," + location1.y + " "
                + location2.x + "," + location2.y + " "
                + parcelServiceDuration + " "
                + parcelCapacity + " "
                + timeWindow;
    }


    private static String makeTimeWindows(long time, Point location1, Point location2) {
        double timeToTravel = getExpectedToTravel();

        long currentTime = time;
        if( time < 0)
            currentTime = 0;

        long arrivalTime = (long) (currentTime + parcelAverageTimeWindowVariation);
        double vari = rng.nextDouble();
        long arrivalTimeWindowBegin = (long) (arrivalTime - vari * parcelAverageTimeWindowVariation);
        //vari = rng.nextDouble();
        long arrivalTimeWindowEnd = (long) (arrivalTime + vari * parcelAverageTimeWindowVariation);

        long deliverTime = (long) (arrivalTime + timeToTravel);
        //vari = rng.nextDouble();
        long deliverTimeWindowBegin = (long) (deliverTime - vari * parcelAverageTimeWindowVariation);
        //vari = rng.nextDouble();
        long deliverTimeWindowEnd = (long) (deliverTime + vari * parcelAverageTimeWindowVariation);

        return arrivalTimeWindowBegin + "/" + arrivalTimeWindowEnd + "=" + deliverTimeWindowBegin + "/" + deliverTimeWindowEnd;

    }

    private static double getExpectedToTravel() {
        return expectedTravelTime + ((2*rng.nextDouble() - 1) * travelTimeVariation);

    }

    private static Point randomPoint(){
        double x = rng.nextDouble() * height;
        double y = rng.nextDouble() * width;
        return new Point(x,y);
    }



    private static int getRequiredAgents() {
        double value = 0;
        double random = rng.nextDouble();
        for(int i = 0; i < maxAmountOfRequiredAgents; i++){
            value += distribution[i];
            if(random < value)
                return i + 1;
        }
        return maxAmountOfRequiredAgents; //FIXME add something to make it randomly generated
    }


}
