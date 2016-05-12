package main;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.*;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.scenario.StopConditions;
import org.apache.commons.math3.random.RandomGenerator;
import scenario.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by KevinB on 8/05/2016.
 */
public class Main {

    static final Point MIN_POINT = new Point(0, 0);
    static final Point MAX_POINT = new Point(10, 10);


    private static final int NUM_DEPOTS = 1;
    private static final int DEPOT_CAPACITY = 1;

    private static final int NUM_VEHICLES = 5;
    private static final int VEHICLE_CAPACITY = 1;
    private static final double VEHICLE_SPEED = 100d;


    private static final int NUM_PARCELS = 1;
    private static final int SERVICE_DURATION = 1000; // in ms
    private static final double NEW_PARCEL_PROB = 0.002;

    public static void main(String[] args) {
        runWithScenario();
    }

    public static void runWithRng() {
        View.Builder viewBuilder = createGUI();

        final Simulator simulator = getBasicSimulatorBuilder(viewBuilder).build();


        final RandomGenerator rng = simulator.getRandomGenerator();

        //initializeDepot(simulator, rng);
        initializeVehicles(simulator, rng);
        initializeParcels(simulator, rng);
        
        configureParcelSpawn(simulator, rng);

        simulator.start();
    }


    public static void runWithScenario() {

        View.Builder viewBuilder = createGUI();

        //final Simulator.Builder simulator = getBasicSimulatorBuilder(viewBuilder);

        int id = 0;
        Scenario myScenario = makeScenario(viewBuilder, id);
//        Path path = FileSystems.getDefault().getPath("src\\main\\resources", "ThreePackageScenario.txt");
//
//        try {
//            ScenarioIO.write(myScenario,path);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        Simulator.builder()
                .addModel(ScenarioController
                                .builder(myScenario)
                        .withEventHandler(NewParcelEvent.class, new NewParcelEventHandler())
                        .withEventHandler(NewVehicleEvent.class, new NewVehicleEventHandler()))
                .build()
                .start();

    }

    private static Simulator.Builder getBasicSimulatorBuilder(View.Builder viewBuilder) {
        return Simulator.builder()
                    .addModel(RoadModelBuilders.plane()
                            .withMinPoint(MIN_POINT)
                            .withMaxPoint(MAX_POINT)
                            .withMaxSpeed(10000d))
                    .addModel(DefaultPDPModel.builder())
                    .addModel(CommModel.builder())
                    .addModel(viewBuilder);
    }

    private static Scenario makeScenario(View.Builder viewBuilder, int id) {
        Scenario.Builder scenarioBuilder = Scenario.builder();

        scenarioBuilder.addModel(RoadModelBuilders.plane()
                .withMinPoint(MIN_POINT)
                .withMaxPoint(MAX_POINT)
                .withMaxSpeed(10000d))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(viewBuilder);

        File file = Paths.get("src\\main\\resources\\scene.txt").toFile();
        long lastEventTime = -1;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                TimedEvent event = TimedEventFactory.makeTimedEventFromString(line);
                if(event != null) {
                    scenarioBuilder.addEvent(event);
                    lastEventTime = event.getTime();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        StopCondition stopCondition= StopConditions.and(StopConditions.limitedTime(lastEventTime+1),new NoParcelsStopCondition());
        scenarioBuilder.setStopCondition(stopCondition);

        return scenarioBuilder.build();
    }


    private static void configureParcelSpawn(final Simulator simulator, final RandomGenerator rng) {
        final RoadModel roadModel = simulator.getModelProvider().getModel(
                RoadModel.class);

        simulator.addTickListener(
                new TickListener() {
                    @Override
                    public void tick(TimeLapse time) {
//                        if (time.getStartTime() > endTime) {
//                            simulator.stop();
//                        } else
                        if (rng.nextDouble() < NEW_PARCEL_PROB) {
                            simulator.register(new MyParcel(
                                    Parcel
                                            .builder(roadModel.getRandomPosition(rng),
                                                    roadModel.getRandomPosition(rng))
                                            .serviceDuration(SERVICE_DURATION)
                                            .neededCapacity(1)
                                            .buildDTO(), rng));
                        }
                    }

                    @Override
                    public void afterTick(TimeLapse timeLapse) {

                    }
                }
        );
    }

    private static List<MyParcel> initializeParcels(Simulator simulator, RandomGenerator rng) {
        final RoadModel roadModel = simulator.getModelProvider().getModel(
                RoadModel.class);

        LinkedList<MyParcel> parcelList = new LinkedList<MyParcel>();

        for (int i = 0; i < NUM_PARCELS; i++) {
            MyParcel parcel = new MyParcel(
                    Parcel.builder(roadModel.getRandomPosition(rng),
                            roadModel.getRandomPosition(rng))
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1)
                            .buildDTO(), rng);
            simulator.register(parcel);
            parcelList.add(parcel);
        }

        return parcelList;
    }

    private static void initializeDepot(Simulator simulator, RandomGenerator rng) {
        final RoadModel roadModel = simulator.getModelProvider().getModel(
                RoadModel.class);

        // add depots, taxis and parcels to simulator
        for (int i = 0; i < NUM_DEPOTS; i++) {
            simulator.register(new MyDepot(roadModel.getRandomPosition(rng),
                    DEPOT_CAPACITY));
        }
    }

    private static void initializeVehicles(Simulator simulator, RandomGenerator rng) {
        final RoadModel roadModel = simulator.getModelProvider().getModel(
                RoadModel.class);

        for (int i = 0; i < NUM_VEHICLES; i++) {
            simulator.register(new MyVehicle(VehicleDTO.builder()
                    .startPosition(roadModel.getRandomPosition(rng))
                    .capacity(VEHICLE_CAPACITY)
                    .speed(VEHICLE_SPEED)
                    .build()));
        }
    }

    private static View.Builder createGUI() {
        View.Builder view = View.builder()
                .with(PlaneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(
                                MyDepot.class, "/graphics/perspective/tall-building-64.png")
                        .withImageAssociation(
                                MyVehicle.class, "/graphics/flat/taxi-32.png")
                        .withImageAssociation(
                                MyParcel.class, "/graphics/flat/person-red-32.png"))
                .with(CommRenderer.builder()
                        .withMessageCount())
                .withTitleAppendix("MAS Project");

        return view;
    }
}
