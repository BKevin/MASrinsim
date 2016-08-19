package mas.scenario;

import com.github.rinde.rinsim.geom.Point;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by KevinB on 12/05/2016.
 */
public class ScenarioGenerator {
    private static Random rng = new Random();
    private static long currentTime;

    private static double width = 10;
    private static double height = 10;

    private static int amountVehiclesAtStart = 10;
    private static int amountParcelsAtStart = 50;
    private static int amountNewVehicles = 0;
    private static int amountNewParcels = 3;


    private static long vehicleAverageInterArrivalTime; //ms
    private static long vehicleInterArrivalTimeVariation; //ms
    private static int vehicleCapacity = 1;
    private static int vehicleSpeed = 10;


    private static long parcelAverageInterArrivalTime = 3750000/5; //ms //doubled
    private static long parcelInterArrivalTimeVariation = 100000; //ms
    private static long parcelAverageTimeWindowVariation = 10000; //ms
    private static int parcelServiceDuration = 0;
    private static int parcelCapacity = 1;
    //Distribution (chances) of required agents: p_1, p_2, p_3
    private static double[] parcelDistribution = {0,0.3,0.3,0.4};

    private static double expectedTravelTime = 255555; //from center to a corner
    private static double travelTimeVariation = 50000;

    public static void main(String[] args) {
        generateScenario(generateFileName(args), amountVehiclesAtStart, amountParcelsAtStart, amountNewParcels, parcelDistribution, parcelAverageInterArrivalTime);
    }

    @NotNull
    private static String generateFileName(String[] filenames) {
        String filename = filenames.length > 0 ? filenames[0] : "scene"
                +"_"+new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date())
                +".txt";

        return "resources/scenario/" + filename;
    }


    public static void generateScenario(String filePath, int maxAgents, int numParcels, int numInitParcels, double[] distribution, long parcelAverageInterArrivalTime) {
        currentTime = 0;

        File file = Paths.get(filePath).toFile();

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter br = new BufferedWriter(
                                            new OutputStreamWriter(
                                                    new FileOutputStream(file), "utf-8"))) {

            String depotEvent = makeNewDepotEventAtStart();
            br.write(depotEvent);
            br.newLine();

            for(int i = 0; i < maxAgents; i++){
                String vehicleEvent = makeVehicleEventAtStart();
                br.write(vehicleEvent);
                br.newLine();
            }
            for(int i = 0; i < numInitParcels; i++){
                String parcelEvent = makeParcelEventAtStart(distribution);
                br.write(parcelEvent);
                br.newLine();
            }

            for(int i = 0; i < amountNewVehicles; i++){
                String vehicleEvent = makeNewVehicleEvent();
                br.write(vehicleEvent);
                br.newLine();
            }
            for(int i = 0; i < numParcels; i++){
                String parcelEvent = makeNewParcelEvent(distribution, parcelAverageInterArrivalTime);
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
                + vehicleSpeed;
    }

    private static String makeParcelEventAtStart(double[] distribution) {
        int requiredAgents = getRequiredAgents(distribution);
        if (requiredAgents == 1)
            return "NewParcel "
                    + parcelEventAtTime(-1);
        else
            return "NewMultiParcel "
                    + parcelEventAtTime(-1) + " "
                    + requiredAgents;
    }

    private static String makeNewParcelEvent(double[] distribution, long parcelAverageInterArrivalTime) {
        int requiredAgents = getRequiredAgents(distribution);
        long rngTime = (long) (currentTime + parcelAverageInterArrivalTime + ((2*rng.nextDouble() - 1) * parcelInterArrivalTimeVariation));
        currentTime = rngTime;
        if (requiredAgents == 1)
            return "NewParcel "
                    + parcelEventAtTime(rngTime);
        else
            return "NewMultiParcel "
                    + parcelEventAtTime(rngTime) + " "
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



    private static int getRequiredAgents(double[] distribution) {
        double value = 0;
        double random = rng.nextDouble();
        for(int i = 0; i < distribution.length; i++){
            value += distribution[i];
            if(random < value)
                return i + 1;
        }
        return distribution.length; //FIXME add something to make it randomly generated
    }


}
