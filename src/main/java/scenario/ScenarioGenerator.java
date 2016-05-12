package scenario;

import com.github.rinde.rinsim.geom.Point;

import java.io.*;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Created by KevinB on 12/05/2016.
 */
public class ScenarioGenerator {
    private static Random rng = new Random();
    private static long currentTime;

    private static double width = 10;
    private static double height = 10;

    private static int amountVehiclesAtStart = 5;
    private static int amountParcelsAtStart = 3;
    private static int amountNewVehicles = 0;
    private static int amountNewParcels = 100;


    private static long vehicleAverageInterArrivalTime; //ms
    private static long vehicleInterArrivalTimeVariation; //ms
    private static int vehicleCapacity = 1;
    private static int vehicleSpeed = 100;


    private static long parcelAverageInterArrivalTime = 50000; //ms
    private static long parcelInterArrivalTimeVariation = 10000; //ms
    private static int parcelServiceDuration = 1000;
    private static int parcelCapacity = 1;

    public static void main(String[] args) {
        generateScenario();
    }

    private static void generateScenario() {
        currentTime = 0;

        File file = Paths.get("src\\main\\resources\\new_scenarios\\scene.txt").toFile();
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
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String makeNewDepotEventAtStart()  {
        return depotEventAtTime(-1);
    }

    private static String depotEventAtTime(long time) {
        return "NewDepot " + time + " " + height/2 + "," + width /2 + " " + amountVehiclesAtStart;
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
        return "NewVehicle " + time + " " + location.x + "," + location.y + " " + vehicleCapacity + " " + vehicleSpeed;
    }

    private static String makeParcelEventAtStart() {
        return parcelEventAtTime(-1);
    }

    private static String makeNewParcelEvent() {
        long rngTime = (long) (currentTime + parcelAverageInterArrivalTime  + ((2*rng.nextDouble() - 1) * parcelInterArrivalTimeVariation));
        currentTime = rngTime;
        return parcelEventAtTime(rngTime);
    }

    private static String parcelEventAtTime(long time) {
        Point location1 = randomPoint();
        Point location2 = randomPoint();
        return "NewParcel " + time + " " + location1.x + "," + location1.y + " " + location2.x + "," + location2.y + " " + parcelServiceDuration + " " + parcelCapacity;
    }

    private static Point randomPoint(){
        double x = rng.nextDouble() * height;
        double y = rng.nextDouble() * width;
        return new Point(x,y);
    }


}
